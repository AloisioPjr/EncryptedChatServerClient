/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package encryptedchat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import static java.lang.System.exit;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.Scanner;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

/**
 *
 * @author Alois
 */
public class EncryptedChatServer {

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(12345);
            System.out.println("Server started. Waiting for clients...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                //System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());

                /*ClientHandler clientHandler = new ClientHandler(clientSocket);
                Thread thread = new Thread(clientHandler);
                thread.start();// the previous statements can also be 
                substituted by the follwing statement*/
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class ClientHandler implements Runnable {

    private Socket socket;
    private BufferedReader reader;
    
    private ObjectInputStream objectInputStream;
    private SecretKey aesKey;
    private BufferedWriter writer;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            objectInputStream = new ObjectInputStream(socket.getInputStream());//to receive the public key

            //System.out.println(Base64.getEncoder().encodeToString(AESKeyEncryptor()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        SendEncryptedAESKey();
        listenForMessage();
        System.out.println("A new client has connected to the server.");
        Scanner scanner = new Scanner(System.in);
        String message;
        while (socket.isConnected()) {
            try {
                message = scanner.nextLine();
                writer.write(EncryptMessage("Server: "+message));
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

    private void closeEverything(Socket socket, BufferedReader reader, BufferedWriter bwriter) {
        try {
            //check if the resources (socket, reader, writer) are still valid before closing them
            if (reader != null) {//
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

    public static SecretKey AESKeyGenerator() {
        SecretKey aesKey = null;
        try {  
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(256);
            aesKey = keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        //System.out.println("aesKey----->" + aesKey);
        return aesKey;
    }

    public PublicKey ReceivedPublicKey() {
        PublicKey receivedPublicKey = null;
        try {
            receivedPublicKey = (PublicKey) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
        }
        //System.out.println("receivedPublicKey----->" + receivedPublicKey);
        return receivedPublicKey;
    }

    public String AESKeyEncryptor() {

        try {
            aesKey = AESKeyGenerator();
            PublicKey receivedPublicKey = ReceivedPublicKey();

            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, receivedPublicKey);
            byte[] encryptedAESKeyBytes = cipher.doFinal(aesKey.getEncoded());//TODO: try changing to .getBytes()
            String encryptedAESKeyString = Base64.getEncoder().encodeToString(encryptedAESKeyBytes);
            //System.out.println("encryptedAESKeyBytes----->" + Arrays.toString(encryptedAESKeyBytes));
            //System.out.println("encryptedAESKeyString----->" + (encryptedAESKeyString));
            return encryptedAESKeyString;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public void SendEncryptedAESKey() {
        try {
        
            String encryptedAESKeyString = AESKeyEncryptor();
            writer.write(encryptedAESKeyString);
            writer.newLine();
            writer.flush();
        } catch (IOException ex) {
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
