/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2030 The XdagJ Developers
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.xdag.wallet;

import io.xdag.config.Config;
import io.xdag.crypto.Bip32ECKeyPair;
import io.xdag.crypto.ECKeyPair;
import io.xdag.crypto.MnemonicUtils;
import io.xdag.crypto.SecureRandomUtils;
import io.xdag.utils.Numeric;
import lombok.extern.slf4j.Slf4j;

import java.io.Console;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;

import static io.xdag.crypto.Bip32ECKeyPair.HARDENED_BIT;

@Slf4j
public class WalletUtils {

    private static final Scanner scanner = new Scanner(new InputStreamReader(System.in, StandardCharsets.UTF_8));

    // https://github.com/satoshilabs/slips/blob/master/slip-0044.md
    public static final int XDAG_BIP44_CION_TYPE = 586;

    public static Bip32ECKeyPair generateBip44KeyPair(Bip32ECKeyPair master, int index) {
        // m/44'/586'/0'/0/0
        // xdag coin type 586 at https://github.com/satoshilabs/slips/blob/master/slip-0044.md
        final int[] path = {44 | HARDENED_BIT, XDAG_BIP44_CION_TYPE | HARDENED_BIT, 0 | HARDENED_BIT, 0, index};
        return Bip32ECKeyPair.deriveKeyPair(master, path);
    }

    public static Bip32ECKeyPair importMnemonic(Wallet wallet, String password, String mnemonic, int index) {
        wallet.unlock(password);
        byte[] seed = MnemonicUtils.generateSeed(mnemonic, password);
        Bip32ECKeyPair masterKeypair = Bip32ECKeyPair.generateKeyPair(seed);
        return generateBip44KeyPair(masterKeypair, index);
    }

    public static ECKeyPair importPrivateKey(Wallet wallet, String password, String privateKeyHexString) {
        wallet.unlock(password);
        ECKeyPair key = ECKeyPair.create(Numeric.toBigInt(privateKeyHexString));
        wallet.addAccount(key);
        return key;
    }

    public static boolean convertOldWallet(Wallet newWallet, String password, OldWallet oldWallet) {
        newWallet.unlock(password);
        List<KeyInternalItem> oldKeyList = oldWallet.getKey_internal();
        for(KeyInternalItem oldKey : oldKeyList) {
            newWallet.addAccount(ECKeyPair.create(oldKey.ecKey.getPrivateKey()));
        }
        return newWallet.flush();
    }

    public static Wallet loadWallet(Config config) {
        return new Wallet(config);
    }

    public static Wallet loadAndUnlockWallet(Config config, String password) {
        Wallet wallet = loadWallet(config);
        if (password == null) {
            if (wallet.unlock("")) {
                wallet.setPassword("");
            } else {
                wallet.setPassword(readPassword("Please enter your password: "));
            }
        }

        if (!wallet.unlock(password)) {
            log.error("Invalid password");
            System.exit(-1);
        }

        return wallet;
    }

    /**
     * Create a new wallet with a new password
     */
    public static Wallet createNewWallet(Config config) {
        String newPassword = readNewPassword("EnterNewPassword:", "ReEnterNewPassword:");
        if (newPassword == null) {
            return null;
        }

        Wallet wallet = loadWallet(config);
        wallet.setPassword(newPassword);

        if (!wallet.unlock(newPassword) || !wallet.flush()) {
            log.error("CreateNewWalletError");
            System.exit(-1);
            return null;
        }

        return wallet;
    }

    /**
     * Read a new password from input and require confirmation
     */
    public static String readNewPassword(String newPasswordMessageKey, String reEnterNewPasswordMessageKey) {
        String newPassword = readPassword(newPasswordMessageKey);
        String newPasswordRe = readPassword(reEnterNewPasswordMessageKey);

        if (!newPassword.equals(newPasswordRe)) {
            log.error("ReEnter NewPassword Incorrect");
            System.exit(-1);
            return null;
        }

        return newPassword;
    }

    /**
     * Reads a line from the console.
     */
    public static String readLine(String prompt) {
        if (prompt != null) {
            System.out.print(prompt);
            System.out.flush();
        }

        return scanner.nextLine();
    }

    public static void initializedHdSeed(Wallet wallet) {
        if (wallet.isUnlocked() && !wallet.isHdWalletInitialized()) {
            // HD Mnemonic
            System.out.println("HdWalletInitialize");
            byte[] initialEntropy = new byte[16];
            SecureRandomUtils.secureRandom().nextBytes(initialEntropy);
            String phrase = MnemonicUtils.generateMnemonic(initialEntropy);
            System.out.println("HdWalletMnemonic:"+ phrase);

            String repeat = readLine("HdWalletMnemonicRepeat:");
            repeat = String.join(" ", repeat.trim().split("\\s+"));

            if (!repeat.equals(phrase)) {
                log.error("HdWalletInitializationFailure");
                System.exit(-1);
                return;
            }

            wallet.initializeHdWallet(phrase);
            wallet.flush();
            log.info("HdWalletInitializationSuccess");
        }
    }

    public static String readPassword(String prompt) {
        Console console = System.console();
        if (console == null) {
            if (prompt != null) {
                System.out.print(prompt);
                System.out.flush();
            }
            return scanner.nextLine();
        }
      return new String(console.readPassword(prompt));
    }
}