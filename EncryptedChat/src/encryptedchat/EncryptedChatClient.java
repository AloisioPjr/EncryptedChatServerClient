/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package encryptedchat;

import java.awt.event.*;
import java.io.*;
import static java.lang.System.exit;
import java.net.*;
import java.security.*;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;

public class EncryptedChatClient {

    public static void main(String[] args) {
            ClientFrame myFrame = new ClientFrame();
            myFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

}

class ClientFrame extends JFrame {

    public ClientFrame() {
        setBounds(600, 300, 280, 350);
        ClientPanel myPanel = new ClientPanel();
        add(myPanel);

        setVisible(true);
    }
}

class ClientPanel extends JPanel {

    private JTextField messageTf;
    private JButton sendBtn;
    private JTextArea messageArea;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public ClientPanel() {

        JLabel text = new JLabel("CHAT");
        add(text);
        messageArea = new JTextArea(12, 20);
        add(messageArea);
        messageTf = new JTextField(20);
        add(messageTf);
        sendBtn = new JButton("Send");
        //ClientSender myEvent = new ClientSender();
        add(sendBtn);

        sendBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String message = messageTf.getText();
                sendMessage(message);
            }
        });
        

        try {
            socket = new Socket("localhost", 12345);
            System.out.println("Connected to server.");

            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            new Thread(new ClientReceiver(socket, this)).start();
           

        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }

    }
    private void sendMessage (String message) {
        try {
            out.writeObject(message);
            out.flush();
            
            messageTf.setText("");
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
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
    private SecretKey aesKey;
    private ClientPanel clientPanel;

    public ClientReceiver(Socket socket, ClientPanel clientPanel) {
        this.socket = socket;
        this.clientPanel = clientPanel;
        try {
            
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
            String message;

            while ((message = (String) objectInputStream.readObject()) != null) {
                System.out.println("Received: " + message);
                if (message.equals("exit")) {
                    exit(0);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(ClientReceiver.class.getName()).log(Level.SEVERE, null, ex);
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

    public void SendPublicKey(PublicKey publicKey1) {
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
            System.out.println("encryptedAESKeyBytes----->" + (aesKey));

        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {
            ex.printStackTrace();
        }

    }
}
