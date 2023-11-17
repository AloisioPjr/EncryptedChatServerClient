/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package encryptedchat;
// all the libraries imported for the program funcionalities

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import static java.lang.System.exit;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
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
 * @author Aloisio Pereira Junior
 */
public class EncryptedChatServer {

    public static void main(String[] args) {
        try {
            // initialise the new server socket passing the port number 12345 as argument
            ServerSocket serverSocket = new ServerSocket(12345);
            System.out.println("Server started. Waiting for clients...");

            while (true) {// This means its an infinit loop 
                // create a socket and assign the value of the serverSocket acceptance
                Socket clientSocket = serverSocket.accept();

                // create an new instance of the class ClientHandler and pass in the socket as an argument
                ClientHandler clientHandler = new ClientHandler(clientSocket);

                // create  new Thread and pass in the instace of the class created above as an argument
                Thread thread = new Thread(clientHandler);
                thread.start();// start the Thread

                // we can also shorten the code above with the following line 
                // new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {////catch I/O exception from the serverSocket.accept()
            System.out.println("Error: Could not start Server-Client Connection");
        }
    }
}
// this class will implement the Runnable interface which means it will have to override the run method
class ClientHandler implements Runnable {
// all the global veriables used in the program

    private Socket socket;
    private BufferedReader reader;
    private SecretKey aesKey;
    private BufferedWriter writer;

    public ClientHandler(Socket socket) {// the constructor takes the socket object as parameter
        this.socket = socket;
        try {
            // Buffered reader and writer for receiving and sending the messages to the client
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            //System.out.println(Base64.getEncoder().encodeToString(aesKeyEncryptor())); // used for debugging
        } catch (IOException e) {// catch in case of exception when creating the buffered reader and the writer
            System.out.println("Error: Could not start Server-Client Communication");// 
            closeEverything(socket, reader, writer);
        }
    }

    @Override
    public void run() {
        // this method sends the encrypted symmetric key to the client
        sendEncryptedAESKey();
        //this method is a cocurrent thread that listen for messages from the client
        listenForMessage();
        // this method can work concurrently witth the method above 
        sendMessage();
    }

    public void sendMessage() {

        System.out.println("A new client has connected to the server.");
        Scanner scanner = new Scanner(System.in);
        String message;
        while (socket.isConnected()) {// while the socket is connected
            try {
                message = scanner.nextLine();// takes the server input and assigns it to the variable "message"
                //call the method encryptMessage and passes in the message as an argument and sends it to the client
                writer.write(encryptMessage("Server: " + message));
                // the new line method is used for the cursor to move to the next line like if we press enter 
                writer.newLine();
                // the flush method sends whatever is in the buffer through the socket straight away.
                writer.flush();
            } catch (IOException ex) {// catches any input/ output exception when writing into the socket
                System.out.println("Error: Could not send message to Client");
                closeEverything(socket, reader, writer);
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
                        msgFromGroupChat = reader.readLine();// receive message from the client 
                        // the message received from the socket is passed as an argument to the method decryptMessage()
                        System.out.println(decryptMessage(msgFromGroupChat));
                    } catch (IOException e) {//catches any I/O exception when readingmessages from buffer
                        System.out.println("Error: There was a problem receiving messages from  Client");
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

    private void closeEverything(Socket socket, BufferedReader reader, BufferedWriter bwriter) {
        try {
            /* This method checkes if the resources are not null then
             * closes all the resources and it shuts downs the program*/
            if (reader != null) {//
                reader.close();
            }
            if (bwriter != null) {
                bwriter.close();
            }
            if (socket != null) {
                socket.close();
            }
            exit(0);
        } catch (IOException e) {
            System.out.println("Error: There was an error when closing the Server-Client connection ");
            exit(0);
        }
    }

    public SecretKey aesKeyGenerator() {
       
        // create aesKey variable outside of try catch
        SecretKey aesKey = null;
        try {
            //variable type Key generator that has a AES instace as value
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            // initialise the key generator passing the key size of 256 as an argument
            keyGenerator.init(256);
            //assig the generated key to a variable
            aesKey = keyGenerator.generateKey();
            //try-catch for in case of exception on using the algorithm of symmetric key generation
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Error: Could not generate Symmetric Key ");
        }
        //System.out.println("aesKey----->" + aesKey);// used for debugging
        return aesKey;// aesKey accessible outside of the try-catch block
    }

    public PublicKey receivedPublicKey() {
      
        PublicKey receivedPublicKey = null;
        try {
            // create a new object input stream and pass in the socket as an argument
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
            //receive the public key object and assign it to a variable 
            receivedPublicKey = (PublicKey) objectInputStream.readObject();
            //the Object Input stream was used only once now it can be closed
            
            //thy-catch block for I/O of line 196 and ClassNotFound for lines 198
        } catch (IOException | ClassNotFoundException ex) {
            ///print an error message on the screen
            System.out.println("Error: Could not receive Public Key from Client");
        }

        //System.out.println("receivedPublicKey----->" + receivedPublicKey);// used for debugging
        return receivedPublicKey;
    }

    public String aesKeyEncryptor() {
      
        try {
            //symmetric key is generated and assignet to the global variable
            aesKey = aesKeyGenerator();
            //public key received from client and assigned to a variable 
            PublicKey receivedPublicKey = receivedPublicKey();
            // cipher object created and with an instance of RSA 
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            //cipher initialised  on encrypt mode passing in the client public key as an argument
            cipher.init(Cipher.ENCRYPT_MODE, receivedPublicKey);
            //pass in the symetric key to be encrypted wit the cipher and assing the result in a byte array variable
            byte[] encryptedAESKeyBytes = cipher.doFinal(aesKey.getEncoded());//
            // convert the byte array of the previous line to String 
            String encryptedAESKeyString = Base64.getEncoder().encodeToString(encryptedAESKeyBytes);
            //System.out.println("encryptedAESKeyBytes----->" + Arrays.toString(encryptedAESKeyBytes));// used for debugging
            //System.out.println("encryptedAESKeyString----->" + (encryptedAESKeyString));// used for debugging
            // return the String type of the encrypted symmetric key 
            return encryptedAESKeyString;
            //catch any exception that may occur during the encryption of the symmetric key
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {
            System.out.println("Error: Could not encrypt Symmetric Key with the clients Public Key");
        }
        return null;
    }

    public void sendEncryptedAESKey() {
      
        try {
            ///assigns the return value of the method aesKeyEncryptor() to a String variable
            String encryptedAESKeyString = aesKeyEncryptor();
            // Buffered writer and reader dont accept byte array
            /* thats why its important to convert the byte array of the encrypted symmetric key 
               to String in the aesKeyEncryptor() method*/
            writer.write(encryptedAESKeyString);
            writer.newLine();// move the cursor to a new line
            writer.flush();// sends across whatever has beed written in the buffer
        } catch (IOException ex) {// catch if any exception occurs when writing in the buffer
            System.out.println("Error: Could not send encrypted Symmetric Key ");
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
}
