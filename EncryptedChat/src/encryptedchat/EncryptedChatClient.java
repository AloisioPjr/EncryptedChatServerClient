/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package encryptedchat;

import java.io.*;
import java.net.*;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class EncryptedChatClient {

    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost", 12345);
            System.out.println("Connected to server.");

            
            new Thread(new ClientReceiver(socket)).start();

            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class ClientReceiver implements Runnable {

    private static PrivateKey privateKey;
    private static PublicKey publicKey;

    private Socket socket;
    private BufferedReader reader;
    private ObjectOutputStream objectOutputStream;
    
    
    private SecretKey aesKey;
    private BufferedWriter writer;

    public ClientReceiver(Socket socket) {
        this.socket = socket;
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            

            objectOutputStream = new ObjectOutputStream(socket.getOutputStream());//to send
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        RSAKeyGenerator();
        SendPublicKey(publicKey);
        DecryptedAESKey();
        listenForMessage();

        Scanner scanner = new Scanner(System.in);
        System.out.println("This is an end-to-end ecypted chat. \nPlease enter your name:");
        String clientName= scanner.nextLine();
        System.out.println("Type your message...");
        String message;
        while (socket.isConnected()) {
            try {
                message = scanner.nextLine();
                writer.write(EncryptMessage(clientName+": "+message));
                writer.newLine();
                writer.flush();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void listenForMessage() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String msgFromGroupChat;
                while (socket.isConnected()) {
                    try {
                        msgFromGroupChat = reader.readLine();
                        System.out.println(DecryptMessage(msgFromGroupChat));
                    } catch (IOException e) {
                        closeEverything(socket, reader, writer);

                    }
                }
            }
        }).start();

    }

    public static void RSAKeyGenerator() {
        KeyPair keyPair = null;
        try {
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
            keyPairGen.initialize(2048);
            keyPair = keyPairGen.generateKeyPair();
            publicKey = keyPair.getPublic();
            //System.out.println("publicKey----->" + publicKey);
            privateKey = keyPair.getPrivate();
            //System.out.println("privateKey----->" + privateKey);
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
    }

    public void SendPublicKey(PublicKey publicKey) {
        try {
            objectOutputStream.writeObject(publicKey);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public byte[] receivedEncryptedAESKey() {
        //String  encryptedAESKeyString;
        try {
           
            String encryptedAESKeyString = reader.readLine();
            byte[] encryptedAESKeyBytes = Base64.getDecoder().decode(encryptedAESKeyString);
            //System.out.println("encryptedAESKeyBytes----->" + Arrays.toString(encryptedAESKeyBytes));
            //System.out.println("encryptedAESKeyBytes----->" + (encryptedAESKeyString));
            return encryptedAESKeyBytes;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public void DecryptedAESKey() {

        try {

            byte[] receivedEncryptedAESKey = receivedEncryptedAESKey();

            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);

            byte[] decryptedAESKeyBytes = cipher.doFinal(receivedEncryptedAESKey);

            aesKey = new SecretKeySpec(decryptedAESKeyBytes, "AES");
            //System.out.println("encryptedAESKeyBytes----->" + (aesKey));

        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {
            ex.printStackTrace();
        }

    }

    public String EncryptMessage(String message) {
        String encryptedMessage = null;
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey);
            byte[] encryptedBytes = cipher.doFinal(message.getBytes());
            encryptedMessage = Base64.getEncoder().encodeToString(encryptedBytes);

        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException ex) {
            ex.printStackTrace();
        }
        return encryptedMessage;
    }

    public String DecryptMessage(String message) {
        String decryptedMessage = null;
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, aesKey);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(message));
            decryptedMessage = new String(decryptedBytes);

        } catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException ex) {
            ex.printStackTrace();
        }
        return decryptedMessage;
    }

    private void closeEverything(Socket socket, BufferedReader reader, BufferedWriter bwriter) {
        try {
            // Close resources (socket, reader, writer)
            if (reader != null) {
                reader.close();
            }
            if (bwriter != null) {
                bwriter.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace(); // Print the stack trace if an IO exception occurs while closing resources
        }
    }
}
