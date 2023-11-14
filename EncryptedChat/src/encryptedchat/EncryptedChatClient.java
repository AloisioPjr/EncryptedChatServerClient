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
import java.util.Base64;
import java.util.Scanner;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class EncryptedChatClient {

    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost", 12345);// connects to the server trough the port 12345
            System.out.println("Connected to server.");// print a message to indicate the connection was successful   
            new Thread(new ServerHandler(socket)).start(); //the socket is passed as an argument when creating a new instance of the class ServerHandler which is passed as an argument for a new thread 

        } catch (IOException e) {// print error message if theres an error with the socket
            System.out.println("Error: Could not start Server-Client Connection");
        }
    }
}

class ServerHandler implements Runnable {// this class will implement the Runnable interface which means it will have to override the run method

    //These are all the global variables used in the program
    private PrivateKey privateKey;// 
    private PublicKey publicKey;// 
    private SecretKey aesKey;
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;

    public ServerHandler(Socket socket) {// the constructor takes the socket as parameter
        this.socket = socket;// initiate the socket with the value of the parameter
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));// initialize the buffered reader and writer
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

        } catch (IOException e) {// catch any error input output error that occur while initializing the buffers 
            System.out.println("Error: Could not start message buffers");
        }
    }

    @Override
    public void run() {// the overriden run method calls the following methods
        RSAKeyGenerator(); // firstly a public and a private key are generated 
        SendPublicKey(publicKey);// the public Key  created previously is sent to the server
        DecryptedAESKey();// this method decrypts the Symmetric key received from the Server by the method receivedEncryptedAESKey()
        listenForMessage();// this method starts a new anonymous Thread and listen to messages while the user is still capable of sending messages they happens concurrently
        sendMessage();// this method anables the client to send messages to the server

    }

    public void sendMessage() {
        /* this method collects the input from the user usig a Scanner 
         * object and sening it through the socket with the buffered writer
         * this all happen inside of a loop while the the socket is connected
         * and inside of a try catch in case of any input/output exception*/
        Scanner scanner = new Scanner(System.in);
        System.out.println("This is an end-to-end encypted chat. \nPlease enter your name:");
        String clientName = scanner.nextLine();
        System.out.println("Type your message...");
        String message;
        while (socket.isConnected()) {
            try {
                message = scanner.nextLine();
                writer.write(EncryptMessage(clientName + ": " + message));
                writer.newLine();
                writer.flush();
            } catch (IOException ex) {
                System.out.println("Error: Could not send message to Server");
            }
        }
        if (socket.isClosed() || !socket.isConnected()) {
            closeEverything(socket, reader, writer);
        }
    }

    public void listenForMessage() {
        /* This method creates a concurrent new Thread that works only listening for messages from the Server and prints it on the screen.
         * like the previous method all of this happen while the socket is connected with a try catch in case of any I/O exception 
         */
        new Thread(new Runnable() {
            @Override
            public void run() {
                String msgFromGroupChat;
                while (socket.isConnected()) {
                    try {
                        msgFromGroupChat = reader.readLine();
                        System.out.println(DecryptMessage(msgFromGroupChat));
                    } catch (IOException e) {
                        System.out.println("Error: Could not receive message from Server");
                        closeEverything(socket, reader, writer);

                    }
                }
                if (socket.isClosed() || !socket.isConnected()) {
                    closeEverything(socket, reader, writer);
                }
            }
        }).start();

    }

    public void RSAKeyGenerator() {
        /* This method generates the public and the private key necessary to encrypt and decrypt the Symetric Key
         * the public and the private key are stored globaly so other methods can access it
         * it happens inside of a try catch in case of exception 
         */
        KeyPair keyPair = null;
        try {
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
            keyPairGen.initialize(2048);
            keyPair = keyPairGen.generateKeyPair();
            publicKey = keyPair.getPublic();
            //System.out.println("publicKey----->" + publicKey); // used for debugging
            privateKey = keyPair.getPrivate();
            //System.out.println("privateKey----->" + privateKey);// used for debugging
        } catch (NoSuchAlgorithmException ex) {
            System.out.println("Error: Could not generate RSA Key Pair");
        }
    }

    public void SendPublicKey(PublicKey publicKey) {
        /* This method take a public key  object as parameter and send the public key object through a 
         * Object Output Stream becaseu the buffered writer does not accept object type 
         *  all inside of a try catch in case of any I/O exception 
         */
        try {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());//to send
            objectOutputStream.writeObject(publicKey);
        } catch (IOException ex) {
            System.out.println("Error: Could not send Public Key to Server");
        }
    }

    public byte[] receivedEncryptedAESKey() {
        /* This method will receive from the buffered reader the String format of the encrypted Symmetric key 
         * and convert it to byte array with Base64 Class and return it. In case of input/output exception there's a try catch 
         */
        try {

            String encryptedAESKeyString = reader.readLine();
            byte[] encryptedAESKeyBytes = Base64.getDecoder().decode(encryptedAESKeyString);
            // 
            //System.out.println("encryptedAESKeyBytes----->" + Arrays.toString(encryptedAESKeyBytes));// used for debugging
            //System.out.println("encryptedAESKeyBytes----->" + (encryptedAESKeyString));// used for debugging
            return encryptedAESKeyBytes;
        } catch (IOException ex) {
            System.out.println("Error: Could not receive encrypted Symmetric Key from Server");
        }
        return null;
    }

    public void DecryptedAESKey() {
        /* This method takes the value byte array that the receivedEncryptedAESKey() method returns and
         * initiates the Cipher on decrypt the Symmetric key with the public key 
         * and assign it to the global variable aesKey
         * all inside of a try catch error */
        try {

            byte[] receivedEncryptedAESKey = receivedEncryptedAESKey();

            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);

            byte[] decryptedAESKeyBytes = cipher.doFinal(receivedEncryptedAESKey);

            aesKey = new SecretKeySpec(decryptedAESKeyBytes, "AES");
            //System.out.println("encryptedAESKeyBytes----->" + (aesKey));// used for debugging

        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {
            System.out.println("Error: Could not decrypt Symmetric Key");
        }

    }

    public String EncryptMessage(String message) {
        /* This method uses the cipher class together with the Symmetric key gathered earlier
         * to encrypt the message passed as a parameter it it also uses the base64 class to 
         * convert the byte array to string and return it. It is all inside of a try catch and 
         * for security reasons none of the errors print stacktrace of the error itself, instead
         * they print a message*/

        String encryptedMessage = null;
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey);
            byte[] encryptedBytes = cipher.doFinal(message.getBytes());
            encryptedMessage = Base64.getEncoder().encodeToString(encryptedBytes);

        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException ex) {
            System.out.println("Error: Could not encrypt message with Symmetric Key ");
        }
        return encryptedMessage;
    }

    public String DecryptMessage(String message) {
        /* This method uses the cipher class together with the Symmetric key stored globaly
         * to decrypt the message passed as a parameter it uses the base64 class to 
         * convert the String message to byte array and then after its passed through the cipher 
         * and then byte array variable is converted to string and its returned.
         * It is all inside of a try catch and for security reasons none of the errors 
         * print stacktrace of the error itself, instead they print a message*/
        String decryptedMessage = null;
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, aesKey);
            
            byte[] decodedBytes = Base64.getDecoder().decode(message);
        
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            decryptedMessage = new String(decryptedBytes);
            
        } catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException
                | NoSuchAlgorithmException | NoSuchPaddingException ex) {
            System.out.println("Error: Could not decrypt message with Symmetric Key ");
        }
        return decryptedMessage;
    }

    private void closeEverything(Socket socket, BufferedReader reader, BufferedWriter writer) {
        /* This method checkes if the resources are not null then
         * closes all the resources and it shuts downs the program*/
        try {

            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.close();
            }
            if (socket != null) {
                socket.close();
            }
            exit(0);
        } catch (IOException e) {
            System.out.println("Error: There was an error when closing the Server-Client connection ");
        }
    }
}
