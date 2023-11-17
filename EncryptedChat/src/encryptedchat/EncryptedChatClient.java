/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package encryptedchat;
// all the libraries imported for the program funcionalities

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

/**
 *
 * @author Aloisio Pereira Junior
 */
public class EncryptedChatClient {

    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost", 12345);// connects to the server trough the port 12345
            System.out.println("Connected to server.");// print a message to indicate the connection was successful   
            //the socket is passed as an argument when creating a new instance of the class ServerHandler 
            //which is passed as an argument for a new thread 
            new Thread(new ServerHandler(socket)).start();
            // print error message if theres any I/O exception at the creation of the socket
        } catch (IOException e) {
            System.out.println("Error: Could not start Server-Client Connection");
        }
    }
}
// this class will implement the Runnable interface which means it will have to override the run method

class ServerHandler implements Runnable {

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
            // initialize the buffered reader and writer for receiving and sending messages
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            // catch any error input output error that occur while initializing the buffers 
        } catch (IOException e) {
            System.out.println("Error: Could not start message buffers");
        }
    }

    @Override
    public void run() {// the overriden run method calls the following methods
        // firstly a public and a private key are generated 
        rsaKeyGenerator();
        // the public Key  created previously is sent to the server
        sendPublicKey(publicKey);
        // this method decrypts the Symmetric key received from the Server 
        // by the method receivedEncryptedAESKey()
        decryptedAESKey();
        // this method starts a new anonymous Thread and listen to messages while 
        // the user is still capable of sending messages they happens concurrently
        listenForMessage();
        // this method anables the client to send messages to the server
        sendMessage();

    }

    public void sendMessage() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("This is an end-to-end encypted chat. \nPlease enter your name:");
        // takes client name and assigns to a variable 
        String clientName = scanner.nextLine();
        System.out.println("Type your message...");
        String message;
        //iterate while the socket connections is still alive
        while (socket.isConnected()) {
            try {
                // takes the server input and assigns it to the variable "message"
                message = scanner.nextLine();
                //call the method encryptMessage and passes in the message as an argument and sends it to the Server
                writer.write(encryptMessage(clientName + ": " + message));
                // the new line method is used for the cursor to move to the next line like if we press enter 
                writer.newLine();
                // the flush method sends whatever is in the buffer through the socket straight away.
                writer.flush();
                // catches any input/ output exception when writing into the socket
            } catch (IOException ex) {
                //print error message
                System.out.println("Error: Could not send message to Server");
            }
        }
        // once the while loop above breaks this method is called to close all the chanels 
        closeEverything(socket, reader, writer);
    }

public void listenForMessage() {
     
        new Thread(new Runnable() {// this anonymous thread that has a anonymous runnable object 
            @Override// the only function of this thread is to listen for messages that come from the socket
            public void run() {
                String msgFromGroupChat;
                while (socket.isConnected()) {// this loop will run while theres a connection with the socket
                    try {
                        msgFromGroupChat = reader.readLine();// receive message from the Server 
                        // the message received from the socket is passed as an argument to the method decryptMessage()
                        System.out.println(decryptMessage(msgFromGroupChat));
                    } catch (IOException e) {//catches any I/O exception when reading messages from buffer
                        System.out.println("Error: There was a problem receiving messages from  Server");
                        // closes all the chanels 
                        closeEverything(socket, reader, writer);
                    }
                }
                //once the while loop abo breaks
                //calls the thethod close everything and passes in the socket, reader and writer as an argument 
                closeEverything(socket, reader, writer);
            }
        }).start();

    }

    public void rsaKeyGenerator() {
        KeyPair keyPair = null;
        try {
            //create a variable type KeyPairGenerator that has a RSA instace as value
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
             // initialise the key generator passing the key pair size of 2448 as an argument
            keyPairGen.initialize(2048);
            //assign the generated key pair to a variable
            keyPair = keyPairGen.generateKeyPair();
            // assign the public key in the global variable
            publicKey = keyPair.getPublic();
            //System.out.println("publicKey----->" + publicKey); // used for debugging
            //assign the private key to to a global variable
            privateKey = keyPair.getPrivate();
            //System.out.println("privateKey----->" + privateKey);// used for debugging
            //try-catch for in case of exception on using the algorithm of key pair generation
        } catch (NoSuchAlgorithmException ex) {
            //print error message
            System.out.println("Error: Could not generate RSA Key Pair");
        }
    }

    public void sendPublicKey(PublicKey publicKey) {
       
        try {
            // create a new object Output stream and pass in the socket as an argument
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());//to send
            //send the publicKey through the socket in the object output stream 
            objectOutputStream.writeObject(publicKey);
            //close the stream since its the only time it will be used in the program
            
            //in case of I/O exception when creating the ObjectOutputStream 
        } catch (IOException ex) {
            //print error message
            System.out.println("Error: Could not send Public Key to Server");
        }
    }

    public byte[] receivedEncryptedAESKey() {
        try {
            // takes the encrypted AES from the buffer reader and assigns to a variable
            String encryptedAESKeyString = reader.readLine();
            //convert the encrypted AES from strinig to byte array using the Base64 class
            byte[] encryptedAESKeyBytes = Base64.getDecoder().decode(encryptedAESKeyString);
            //System.out.println("encryptedAESKeyBytes----->" + Arrays.toString(encryptedAESKeyBytes));// used for debugging
            //System.out.println("encryptedAESKeyBytes----->" + (encryptedAESKeyString));// used for debugging
            // return the encrypted bytes value of the AES 
            return encryptedAESKeyBytes;
            //in case of I/O exception while using the buffered reader
        } catch (IOException ex) {
            //print error message
            System.out.println("Error: Could not receive encrypted Symmetric Key from Server");
        }
        //return null if an exception occurs
        return null;
    }

    public void decryptedAESKey() {
        try {

            byte[] receivedEncryptedAESKey = receivedEncryptedAESKey();
             //create and initiate the cipher object assigning the RSA parameters to it 
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            // initilize the cipher on decrypt mode passing in the private key as an argument
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            //apply the cipher object to he encrypted AES bytes and assign it to a bite array variable
            byte[] decryptedAESKeyBytes = cipher.doFinal(receivedEncryptedAESKey);
            // rebuild the SecretKeySpec from the bytes and the AES parameter and assign it to the global variable
            aesKey = new SecretKeySpec(decryptedAESKeyBytes, "AES");
            //System.out.println("encryptedAESKeyBytes----->" + (aesKey));// used for debugging

        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                IllegalBlockSizeException | BadPaddingException ex) {
            System.out.println("Error: Could not decrypt Symmetric Key");
        }

    }

  public String encryptMessage(String message) {// takes a String as parameter
      
        String encryptedMessage = null;
        try {
            //create and initiate the cipher object assigning the AES incryption instance  
            Cipher cipher = Cipher.getInstance("AES");
            //initialize the cipher on encrypt mode and passing the Symmetric Key as parameter
            cipher.init(Cipher.ENCRYPT_MODE, aesKey);
            //pass the message as an argument to the cipher object and assign it to a byte array variable 
            byte[] encryptedBytes = cipher.doFinal(message.getBytes());
            //convert the byteArray to String so the buffered writer can send it through the socket
            encryptedMessage = Base64.getEncoder().encodeToString(encryptedBytes);
            /*catch any exception that may occur during the creation of the cipher object,
           encryption of the message and convertion from byte array to String type */
        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
                | IllegalBlockSizeException | BadPaddingException ex) {
            System.out.println("Error: Could not encrypt message with Symmetric Key ");
        }// return the String type encrypted message 
        return encryptedMessage;
    }

    public String decryptMessage(String message) {
     
        String decryptedMessage = null;
        try {
            //create and initiate the cipher object assigning the AES incryption instance  
            Cipher cipher = Cipher.getInstance("AES");
            //initialize the cipher on decrypt mode and passing the Symmetric Key as parameter
            cipher.init(Cipher.DECRYPT_MODE, aesKey);
            //Convert the string type message to array byte 
            byte[] decodedBytes = Base64.getDecoder().decode(message);
            //apply the cipher object to he messagebytes to decrypt and assign it to a variable
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            //create a new string with the decypted bytes
            decryptedMessage = new String(decryptedBytes);
            /*catch any exception that may occur during the creation of the cipher object,
              decryption of the message and convertion from String to byte array type */
        } catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException
                | NoSuchAlgorithmException | NoSuchPaddingException ex) {
            //print error message
            System.out.println("Error: Could not decrypt message with Symmetric Key ");
        }
        return decryptedMessage;// return decrypted message 
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
            
        } catch (IOException e) {
            System.out.println("Error: There was an error when closing the Server-Client connection ");
        }
        exit(0);
    }
}
