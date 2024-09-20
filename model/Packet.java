package model;

import java.util.ArrayList;
import java.util.List;

import control.mainController;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Line;

public class Packet extends Thread {
    private ImageView packetImage;
    private ImageView destinationNode;
    private Line edge;
    private mainController controller;
    private ImageView sourceNode;
    private boolean delivered = false;

    // construtor de pacote
    public Packet(ImageView sourceNode, ImageView destinationNode, Line edge, mainController controller, int ttl) {
        this.sourceNode = sourceNode;
        this.destinationNode = destinationNode;
        this.edge = edge;
        this.controller = controller;

        // inicializando img do pacote
        Image packetImg = new Image("./assets/pacote.png");
        packetImage = new ImageView(packetImg);
        packetImage.setFitWidth(40);
        packetImage.setFitHeight(40);

        // posiciona o pacote na posicao inicial
        packetImage.setLayoutX(sourceNode.getLayoutX() + sourceNode.getFitWidth() / 2 - packetImage.getFitWidth() / 2);
        packetImage
                .setLayoutY(sourceNode.getLayoutY() + sourceNode.getFitHeight() / 2 - packetImage.getFitHeight() / 2);
        Platform.runLater(() -> controller.getMainPane().getChildren().add(packetImage));
    }

    @Override
    public void run() {
        // move o pacote ao longo da aresta
        double startX = edge.getStartX();
        double startY = edge.getStartY();
        double endX = edge.getEndX();
        double endY = edge.getEndY();

        int steps = 100; // etapas da animacao
        for (int i = 0; i <= steps; i++) {
            final double t = (double) i / steps;
            final double currentX = startX + t * (endX - startX);
            final double currentY = startY + t * (endY - startY);

            Platform.runLater(() -> {
                packetImage.setLayoutX(currentX - packetImage.getFitWidth() / 2);
                packetImage.setLayoutY(currentY - packetImage.getFitHeight() / 2);
                controller.getMainPane().requestLayout();
            });

            try {
                Thread.sleep(20); // velocidade da animacao
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Platform.runLater(() -> controller.getMainPane().getChildren().remove(packetImage));

        // necessario para verificar se o remetente recebeu o pacote
        if (destinationNode == controller.getRemetenteNode()) {
            Platform.runLater(() -> destinationNode.setImage(new Image("./assets/nó.png")));
        } else {
            // logica implementada para funcionalidade da opcao 2
            int destinationId = controller.getNodeId(destinationNode);
            int emissorId = controller.getNodeId(sourceNode);

        }
    }

    public boolean isDelivered() {
        return delivered;
    }
}