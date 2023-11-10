/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package encryptedchat;
// java libraries
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
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class EncryptedChatClient { //client class

    public static void main(String[] args) { // main method for the client
        try {
            //create a new socket connection to the server
            Socket socket = new Socket("localhost", 12345);
            System.out.println("Connected to server.");
            //input and output streams for communication
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in)); //read from keyboard
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            //starting new thread to handle messages from server
            new Thread(new ClientReceiver(socket)).start(); //starting new thread is it possible to connect many clients
            //read from the user keyboard and send to the server
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
    //variables for RSA key pair and ES key
    private static PrivateKey privateKey;
    private static PublicKey publicKey;

    private Socket socket;
    private BufferedReader reader;
    private ObjectOutputStream objectOutputStream;
    private ObjectInputStream objectInputStream;
    //private PublicKey publicKey;
    private PrintWriter writer;
    private SecretKey aesKey;
    //constructor for the client receiver
    public ClientReceiver(Socket socket) {
        this.socket = socket;
        try {
            //input and output streams
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);

            objectOutputStream = new ObjectOutputStream(socket.getOutputStream());//to send
            objectInputStream = new ObjectInputStream(socket.getInputStream());//to receive
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //run method for incoming messages
    @Override
    public void run() {
        RSAKeyGenerator();
        SendPublicKey(publicKey);
        //receive and decrypt AES key from server
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
    //generating RSA key pair
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
    
    //send public key to the server
    public void SendPublicKey(PublicKey publicKey) {
        try {
            objectOutputStream.writeObject(publicKey);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    //receive encrypted AES key from the server
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
    //Decrypt AES key using RSA
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
}
