/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package encryptedchat;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import static java.lang.System.exit;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
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
// Generates an AES key, encrypt with the client public key, and sends again to the client.
public class EncryptedChatServer {

    public static void main(String[] args) {
        try {
            //socket to listen for client connections
            ServerSocket serverSocket = new ServerSocket(12345);
            System.out.println("Server started. Waiting for clients...");

            while (true) {
                //accept client connections
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());

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
//class to generates an AES key, encrypts with client public key and send to client
class ClientHandler implements Runnable {

    private Socket clientSocket;
    private BufferedReader reader;
    private PrintWriter writer;
    private ObjectOutputStream objectOutputStream;
    private ObjectInputStream objectInputStream;
    private SecretKey aesKey;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        try {
            reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            writer = new PrintWriter(clientSocket.getOutputStream(), true);

            objectOutputStream = new ObjectOutputStream(clientSocket.getOutputStream());//to send
            objectInputStream = new ObjectInputStream(socket.getInputStream());//to receive

            //System.out.println(Base64.getEncoder().encodeToString(AESKeyEncryptor()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        //send the encrypted aes key to the client
        SendEncryptedAESKey();

        try {
            //listen for incoming messages
            String message = reader.readLine();
            //if !(message.toLowerCase().trim().equals("exit"))||
            while (((message != null))) {
                System.out.println("Received: " + message);
                // Broadcast the message to all clients
                // Modify this part to send messages to spe cific clients if needed
            }
    
            if (message.equals("exit")) {

                exit(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
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
        System.out.println("aesKey----->" + aesKey);
        return aesKey;
    }

    public PublicKey ReceivedPublicKey() {
        PublicKey receivedPublicKey = null;
        try {
            receivedPublicKey = (PublicKey) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
        }
        System.out.println("receivedPublicKey----->" + receivedPublicKey);
        return receivedPublicKey;
    }
    //encrypt the AES key with the client public key
    public byte[] AESKeyEncryptor() {

        try {
            aesKey = AESKeyGenerator();
            PublicKey receivedPublicKey = ReceivedPublicKey();

            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, receivedPublicKey);
            byte[] encryptedAESKeyBytes = cipher.doFinal(aesKey.getEncoded());//TODO: try changing to .getBytes()

            System.out.println("encryptedAESKeyBytes----->" + Arrays.toString( encryptedAESKeyBytes));

            return encryptedAESKeyBytes;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {
            ex.printStackTrace();
        }
        return null;
    }
    //encrypt the AES key
    public void SendEncryptedAESKey() {
        try {
            byte[] encryptedAESKeyBytes = AESKeyEncryptor();
            // Send the length of the encrypted key and then the key bytes to the client
            OutputStream outputStream = clientSocket.getOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
            dataOutputStream.writeInt(encryptedAESKeyBytes.length);
            dataOutputStream.write(encryptedAESKeyBytes);
            dataOutputStream.flush();
            
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
