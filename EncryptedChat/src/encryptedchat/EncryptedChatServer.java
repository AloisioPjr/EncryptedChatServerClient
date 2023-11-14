/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package encryptedchat;

import java.io.*;
import static java.lang.System.exit;
import java.net.*;
import java.security.*;
import java.util.Arrays;
import javax.crypto.*;
import javax.swing.*;

/**
 *
 * @author Alois
 */
public class EncryptedChatServer {

    public static void main(String[] args) {

        ServerFrame myFrame = new ServerFrame();

        myFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        try {
            ServerSocket serverSocket = new ServerSocket(12345);
            System.out.println("Server started. Waiting for clients...");

            while (true) {
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

class ServerFrame extends JFrame {

    private JTextArea messageArea;

    public ServerFrame() {
        setBounds(1200, 300, 280, 350);

        ServerPanel myPanel = new ServerPanel();
        messageArea = myPanel.getMessageArea();
        add(myPanel);

        setVisible(true);
    }

    

}

class ServerPanel extends JPanel {

    private JTextField messageTf;
    private JButton sendBtn;
    private JTextArea messageArea;

    public ServerPanel() {

        JLabel text = new JLabel("CHAT (server)");
        add(text);
        messageArea = new JTextArea(12, 20);
        add(messageArea);
        messageTf = new JTextField(20);
        add(messageTf);
        sendBtn = new JButton("Send");
        //ClientHandler myEvent = new ClientHandler();
        add(sendBtn);

    }

    JTextArea getMessageArea() {
        return messageArea;
    }

}

class ClientHandler implements Runnable {

    private Socket clientSocket;
    private BufferedReader reader;
    private PrintWriter writer;
    private ObjectOutputStream objectOutputStream;
    private ObjectInputStream objectInputStream;
    private SecretKey aesKey;
    private JTextArea messageArea;

    public ClientHandler(Socket socket) {

        this.clientSocket = socket;
        this.messageArea = messageArea;
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
        SendEncryptedAESKey();

        try {

            String message;
            //if !(message.toLowerCase().trim().equals("exit"))||
            while ((message = reader.readLine()) != null) {
                System.out.println("Received: " + message);
                // Broadcast the message to all clients
                // Modify this part to send messages to spe cific clients if needed
                appendToMessageArea("Received: " + message);
            }

            if (message == null || message.equals("exit")) {

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

    public byte[] AESKeyEncryptor() {

        try {
            aesKey = AESKeyGenerator();
            PublicKey receivedPublicKey = ReceivedPublicKey();

            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, receivedPublicKey);
            byte[] encryptedAESKeyBytes = cipher.doFinal(aesKey.getEncoded());//TODO: try changing to .getBytes()

            System.out.println("encryptedAESKeyBytes----->" + Arrays.toString(encryptedAESKeyBytes));

            return encryptedAESKeyBytes;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public void SendEncryptedAESKey() {
        try {
            byte[] encryptedAESKeyBytes = AESKeyEncryptor();

            OutputStream outputStream = clientSocket.getOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
            dataOutputStream.writeInt(encryptedAESKeyBytes.length);
            dataOutputStream.write(encryptedAESKeyBytes);
            dataOutputStream.flush();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void appendToMessageArea(String message) {
        SwingUtilities.invokeLater(() -> {
            messageArea.append(message + "\n");

        });
    }
}
