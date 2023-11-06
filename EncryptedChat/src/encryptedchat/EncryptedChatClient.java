/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package encryptedchat;

import java.io.*;
import static java.lang.System.exit;
import java.net.*;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Base64;
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

            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

            new Thread(new ClientReceiver(socket)).start();

            String message;
            while ((message = reader.readLine()) != null) {
                writer.println(message);
            }
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
    private ObjectInputStream objectInputStream;
    //private PublicKey publicKey;
    private PrintWriter writer;
    private SecretKey aesKey;

    public ClientReceiver(Socket socket) {
        this.socket = socket;
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);

            objectOutputStream = new ObjectOutputStream(socket.getOutputStream());//to send
            objectInputStream = new ObjectInputStream(socket.getInputStream());//to receive
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        RSAKeyGenerator();
        SendPublicKey(publicKey);

        DecryptedAESKey();
        
        try {
            String message = reader.readLine();

            while ((message != null)) {
                System.out.println("Received: " + message);
            }
            if (message.equals("exit")) {

                exit(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void RSAKeyGenerator() {
        KeyPair keyPair = null;
        try {
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
            keyPairGen.initialize(2048);
            keyPair = keyPairGen.generateKeyPair();
            publicKey = keyPair.getPublic();
            System.out.println("publicKey----->" + publicKey);
            privateKey = keyPair.getPrivate();
        System.out.println("privateKey----->" + privateKey);
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
            InputStream inputStream = socket.getInputStream();
            DataInputStream dataInputStream = new DataInputStream(inputStream);
            int EncryptedAESKeyLength = dataInputStream.readInt();
            byte[] encryptedAESKeyBytes = new byte[EncryptedAESKeyLength];
            dataInputStream.readFully(encryptedAESKeyBytes);
            System.out.println("encryptedAESKeyBytes----->" + Arrays.toString(encryptedAESKeyBytes));
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
            System.out.println("encryptedAESKeyBytes----->" +(aesKey));
           

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
}
