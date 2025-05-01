package org.ensate;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        Socket socket = null;
        InputStreamReader inputStreamReader = null;
        OutputStreamWriter outputStreamWriter = null;
        BufferedReader bufferedReader = null;
        BufferedWriter bufferedWriter = null;

        try {
            System.out.println("Tentative de connexion au serveur...");
            socket = new Socket("localhost", 1234);
            System.out.println("Connecté au serveur!");

            inputStreamReader = new InputStreamReader(socket.getInputStream());
            outputStreamWriter = new OutputStreamWriter(socket.getOutputStream());

            bufferedReader = new BufferedReader(inputStreamReader);
            bufferedWriter = new BufferedWriter(outputStreamWriter);

            // Lire le message de bienvenue du serveur
            String welcomeMsg = bufferedReader.readLine();
            System.out.println("Serveur: " + welcomeMsg);

            Scanner scanner = new Scanner(System.in);
            System.out.println("Commencez à chatter (tapez 'BYE' pour quitter):");

            while (true) {
                System.out.print("Vous: ");
                String msgToSend = scanner.nextLine();

                bufferedWriter.write(msgToSend);
                bufferedWriter.newLine();
                bufferedWriter.flush();

                String serverResponse = bufferedReader.readLine();
                System.out.println("Serveur: " + serverResponse);

                if (msgToSend.equalsIgnoreCase("BYE")) {
                    System.out.println("Déconnexion du serveur...");
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Erreur de connexion: " + e.getMessage());
        } finally {
            try {
                if (socket != null) socket.close();
                if (inputStreamReader != null) inputStreamReader.close();
                if (outputStreamWriter != null) outputStreamWriter.close();
                if (bufferedReader != null) bufferedReader.close();
                if (bufferedWriter != null) bufferedWriter.close();
                System.out.println("Déconnecté du serveur.");
            } catch (IOException e) {
                System.out.println("Erreur lors de la fermeture des ressources: " + e.getMessage());
            }
        }
    }
}