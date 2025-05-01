package org.ensate;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public static void main(String[] args) throws IOException {
        Socket socket = null;
        InputStreamReader inputStreamReader = null;
        OutputStreamWriter outputStreamWriter = null;
        BufferedReader bufferedReader = null;
        BufferedWriter bufferedWriter = null;
        ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(1234);
            System.out.println("Serveur démarré. En attente de connexions...");

            while(true) {
                try {
                    socket = serverSocket.accept();
                    System.out.println("Nouveau client connecté: " + socket.getInetAddress().getHostAddress());

                    inputStreamReader = new InputStreamReader(socket.getInputStream());
                    outputStreamWriter = new OutputStreamWriter(socket.getOutputStream());

                    bufferedReader = new BufferedReader(inputStreamReader);
                    bufferedWriter = new BufferedWriter(outputStreamWriter);

                    bufferedWriter.write("Bienvenue sur le serveur de chat!");
                    bufferedWriter.newLine();
                    bufferedWriter.flush();

                    while (true) {
                        String msgFromClient = bufferedReader.readLine();

                        if (msgFromClient == null) {
                            System.out.println("Client déconnecté: " + socket.getInetAddress().getHostAddress());
                            break;
                        }

                        System.out.println("Client [" + socket.getInetAddress().getHostAddress() + "]: " + msgFromClient);

                        if (msgFromClient.equalsIgnoreCase("BYE")) {
                            bufferedWriter.write("Au revoir! Connexion terminée.");
                            bufferedWriter.newLine();
                            bufferedWriter.flush();
                            System.out.println("Client [" + socket.getInetAddress().getHostAddress() + "] a quitté le chat.");
                            break;
                        } else {
                            bufferedWriter.write("Message reçu.");
                            bufferedWriter.newLine();
                            bufferedWriter.flush();
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Erreur de connexion avec le client: " + e.getMessage());
                } finally {
                    try {
                        if (socket != null) socket.close();
                        if (inputStreamReader != null) inputStreamReader.close();
                        if (outputStreamWriter != null) outputStreamWriter.close();
                        if (bufferedReader != null) bufferedReader.close();
                        if (bufferedWriter != null) bufferedWriter.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Erreur au démarrage du serveur: " + e.getMessage());
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        }
    }
}