/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package encryptedchat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
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
    
    
    private SecretKey aesKey;
    private BufferedWriter writer;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            

            //System.out.println(Base64.getEncoder().encodeToString(AESKeyEncryptor())); // used for debugging
        } catch (IOException e) {
            System.out.println("Error: Could not start Server-Client Connection");
        }
    }

    @Override
    public void run() {
        SendEncryptedAESKey();
        listenForMessage();
        sendMessage();
    }
    public void sendMessage() {
        /* this method collects the input from the Server usig a Scanner 
         * object and sending it through the socket with the buffered writer
         * this all happen inside of a loop while the the socket is connected
         * and inside of a try catch in case of any input/output exception*/
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
                System.out.println("Error: Could not send or receive message to Client");
            }
        }if (socket.isClosed()||!socket.isConnected()){
                    closeEverything(socket, reader, writer);
                }
    }

    public void listenForMessage() {
        /* This method creates a concurrent new Thread that works only listening for messages from the Client and prints it on the screen.
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
                        closeEverything(socket, reader, writer);

                    }
                }if (socket.isClosed()||!socket.isConnected()){
                    closeEverything(socket, reader, writer);
                }
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
        } catch (IOException e) {
            System.out.println("Error: There was an error when closing the Server-Client connection ");
        }
    }

    public static SecretKey AESKeyGenerator() {
        /* This method generates the Symmetric Key size of 256 bytes and
         * returns the SecretKey Object. It has a try catch in case of any error 
         * during the generation of the key*/
        SecretKey aesKey = null;
        try {  
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(256);
            aesKey = keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Error: Could not generate Symmetric Key ");
        }
        //System.out.println("aesKey----->" + aesKey);// used for debugging
        return aesKey;
    }

    public PublicKey ReceivedPublicKey() {
        /* THis method receive the Prublic key from the Client through the socket in Object Input Stream 
         * Then its assigned to a Public Key type of variable and returned it
         * Try chatch are implemented in case of errors */
        PublicKey receivedPublicKey = null;
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());//to receive the public key
            receivedPublicKey = (PublicKey) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException ex) {
            System.out.println("Error: Could not receive Public Key from Client");
        }
        //System.out.println("receivedPublicKey----->" + receivedPublicKey);// used for debugging
        return receivedPublicKey;
    }

    public String AESKeyEncryptor() {
        /* This method assigns the value of the Symmetric key from the method AESKeyGenerator() to the global variable aesKey and
         * asssigns the value of the ReceivedPublicKey() to a local variable and
         * encrypts using the cipher class to encrypt the Symmetric key with with the public Key 
         * and returns the string value of the encrypted symetric key
         * Try chatch are implemented in case of errors */
        try {
            aesKey = AESKeyGenerator();
            PublicKey receivedPublicKey = ReceivedPublicKey();

            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, receivedPublicKey);
            byte[] encryptedAESKeyBytes = cipher.doFinal(aesKey.getEncoded());//
            String encryptedAESKeyString = Base64.getEncoder().encodeToString(encryptedAESKeyBytes);
            //System.out.println("encryptedAESKeyBytes----->" + Arrays.toString(encryptedAESKeyBytes));// used for debugging
            //System.out.println("encryptedAESKeyString----->" + (encryptedAESKeyString));// used for debugging
            return encryptedAESKeyString;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {
            System.out.println("Error: Could not encrypt Symmetric Key with the clients Public Key");
        }
        return null;
    }

    public void SendEncryptedAESKey() {
        /* THis method assisgns to a vriable the value returned by the method AESKeyEncryptor()
         * then it uses the buffered writer to send it though the socket
         * Try chatch are implemented in case of errors */
        try {
        
            String encryptedAESKeyString = AESKeyEncryptor();
            writer.write(encryptedAESKeyString);
            writer.newLine();
            writer.flush();
        } catch (IOException ex) {
            System.out.println("Error: Could not send encrypted Symmetric Key ");
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
         * and then byte array variable is converted to string which is then returned as the final decrypted message.
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
}
