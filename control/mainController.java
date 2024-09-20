package control;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import model.Packet;
import javafx.scene.layout.AnchorPane;
import javafx.scene.image.Image;

public class mainController implements Initializable {

    @FXML
    private AnchorPane mainPane; // anchorpane principal do codigo

    @FXML
    private ImageView iniciar;

    @FXML
    private ImageView titulo;

    @FXML
    private ImageView backgroundImg;

    @FXML
    private ImageView emissorReceptor;

    @FXML
    private ImageView botaoInicio;

    @FXML
    private ImageView emissorImg;

    @FXML
    private ImageView remetenteImg;
    @FXML
    private ImageView createdPacks;

    @FXML
    private ImageView spongeUp;

    int iniciouDjikstra = 0; // opcao padrao

    // utilizado para garantir a sincronizacao entre as threads
    private AtomicInteger totalPackets = new AtomicInteger(0);

    @FXML
    private Text packetCounterText;

    private Map<Integer, ImageView> nodes = new HashMap<>();
    private Map<String, Line> edges = new HashMap<>();
    private ImageView backgroundImageView; // referencia do backgroundImg

    private ImageView emissorNode = null; // no emissor
    private ImageView remetenteNode = null; // no receptor

    // metodo principal utilizado para gerar o grafo na tela
    private void createAndShowGraph(int numberOfNodes) {

        // os metodos abaixo servem para caso os recursos visuais sejam apagados em
        // algum momento, eles sejam
        // readicionados no mainPane
        Platform.runLater(() -> {
            if (!mainPane.getChildren().contains(packetCounterText)) {
                mainPane.getChildren().add(packetCounterText);
            }
        });

        Platform.runLater(() -> {
            if (!mainPane.getChildren().contains(botaoInicio)) {
                mainPane.getChildren().add(botaoInicio);
            }
        });

        // apenas remove createdPacks se for necessário limpar
        mainPane.getChildren()
                .removeIf(node -> (node instanceof ImageView && node != backgroundImageView && node != createdPacks)
                        || node == packetCounterText);

        if (mainPane == null) {
            return;
        }

        Platform.runLater(() -> {
            if (!mainPane.getChildren().contains(emissorReceptor)) {
                mainPane.getChildren().add(emissorReceptor);
            }
        });

        // limpa nos e arestas, mantendo a imagem do fundo
        mainPane.getChildren().removeIf(node -> node instanceof ImageView && node != backgroundImageView);
        nodes.clear();
        edges.clear();

        // caso a imagem do fundo tenha se perdido, ela e readicionada
        if (backgroundImageView == null) {
            Image backgroundImage = new Image("file:./assets/backgroundImg.png");
            backgroundImageView = new ImageView(backgroundImage);
            backgroundImageView.setFitWidth(mainPane.getWidth());
            backgroundImageView.setFitHeight(mainPane.getHeight());
            mainPane.getChildren().add(backgroundImageView);
        }

        double radius = 250; // raio do círculo
        double centerX = 450; // centro do círculo em X
        double centerY = 330; // centro do círculo em Y
        double nodeSize = 150; // tamanho dos nós (largura e altura)

        for (int i = 1; i <= numberOfNodes; i++) {
            Image nodeImage;
            try {
                nodeImage = new Image("file:./assets/nó.png");
            } catch (Exception e) {
                continue;
            }

            ImageView nodeImageView = new ImageView(nodeImage);
            nodeImageView.setId("node" + i);
            nodeImageView.setFitWidth(nodeSize);
            nodeImageView.setFitHeight(nodeSize);

            // calcula a posição dos nos no circulo
            double angle = 2 * Math.PI * i / numberOfNodes;
            double x = centerX + radius * Math.cos(angle) - nodeSize / 2;
            double y = centerY + radius * Math.sin(angle) - nodeSize / 2;

            nodeImageView.setLayoutX(x);
            nodeImageView.setLayoutY(y);

            // adiciona o no e o numero ao mainPane
            mainPane.getChildren().addAll(nodeImageView);

            nodes.put(i, nodeImageView);

            // evento de clique necessario para escolher no emissor e receptor
            nodeImageView.setOnMouseClicked(event -> nodeClicked(nodeImageView));
        }

        // caso createdPacks foi removido, e readicionado
        if (!mainPane.getChildren().contains(createdPacks)) {
            mainPane.getChildren().add(createdPacks);
        }
    }

    // metodo utilizado para criar arestas entre os nos
    public void createEdges() {
        for (Line edge : edges.values()) {
            if (!mainPane.getChildren().contains(edge)) {
                mainPane.getChildren().add(edge);
            }
        }
    }

    // metodo que le o arquivo texto backbone.txt, util para organizar o grafo
    private void loadGraphFromFile() {
        try (BufferedReader br = new BufferedReader(new FileReader("./backbone.txt"))) {
            String line = br.readLine();
            if (line != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    int numberOfNodes = Integer.parseInt(line.split(";")[0].trim());

                    // Limpa e prepara os nós e arestas antes de criar um novo gráfico
                    createAndShowGraph(numberOfNodes);

                    while ((line = br.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty()) {
                            String[] parts = line.split(";");
                            if (parts.length == 3) {
                                int node1 = Integer.parseInt(parts[0].trim());
                                int node2 = Integer.parseInt(parts[1].trim());
                                int weight = Integer.parseInt(parts[2].trim());
                                createEdge(node1, node2, weight);
                            }
                        }
                    }

                    // Mostrar as arestas após a criação dos nós
                    createEdges();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    // metodo visual para criacao de arestas
    private void createEdge(int node1, int node2, int weight) {
        ImageView node1View = nodes.get(node1);
        ImageView node2View = nodes.get(node2);

        if (node1View != null && node2View != null) {
            Line edge = new Line();
            edge.setStartX(node1View.getLayoutX() + node1View.getFitWidth() / 2);
            edge.setStartY(node1View.getLayoutY() + node1View.getFitHeight() / 2);
            edge.setEndX(node2View.getLayoutX() + node2View.getFitWidth() / 2);
            edge.setEndY(node2View.getLayoutY() + node2View.getFitHeight() / 2);
            edge.setStroke(Color.BLACK);
            edge.setStrokeWidth(2);

            // Armazena a aresta bidirecional
            edges.put(node1 + "-" + node2, edge);
            edges.put(node2 + "-" + node1, edge);

            Platform.runLater(() -> {
                if (!mainPane.getChildren().contains(edge)) {
                    mainPane.getChildren().add(edge);
                }
            });
        } else {
        }
    }

    // metodo que implementa a logica do usuario clicar em um no para escolhe-lo
    // como emissor ou remetente
    private void nodeClicked(ImageView nodeImageView) {
        if (emissorNode == null) {
            emissorNode = nodeImageView;
            emissorNode.setImage(new Image("file:./assets/emissor.png")); // atualiza a imagem do emissor
        } else if (remetenteNode == null && nodeImageView != emissorNode) {
            remetenteNode = nodeImageView;
            remetenteNode.setImage(new Image("file:./assets/remetente.png")); // atualiza a imagem do remetente

            // iniciar o envio dos pacotes somente apos selecionar emissor e remetente
            if (iniciouDjikstra == 1) {
                emissorReceptor.setVisible(false);
                // aqui implementa o metodo de djikstra
                System.out.println("iniciou djikstra!!" + getIniciouDjikstra());
            }
        } else {
        }
    }

    public Map<Integer, ImageView> getNodes() {
        return nodes; // retorna o mapa de nos
    }

    public int getIniciouDjikstra() {
        return iniciouDjikstra;
    }

    public void setIniciouDjikstra(int iniciouDjikstra) {
        this.iniciouDjikstra = iniciouDjikstra;
    }

    public int getNodeId(ImageView nodeImageView) {
        for (Map.Entry<Integer, ImageView> entry : nodes.entrySet()) {
            if (entry.getValue() == nodeImageView) {
                return entry.getKey(); // retorna id do no
            }
        }
        return -1; // retorna -1 se o no nao for encontrado
    }

    public Map<String, Line> getEdges() {
        return edges; // retorna o mapa de arestas
    }

    public AnchorPane getMainPane() {
        return mainPane;
    }

    public ImageView getRemetenteNode() {
        return remetenteNode;
    }

    // metodos que implementam a logica dos botoes

    @FXML
    void clicouIniciar(MouseEvent event) {
        titulo.setVisible(false);
        setIniciouDjikstra(1);
        buttonOff();
        loadGraphFromFile();
        packetCounterText.setVisible(true);
        createdPacks.setVisible(true);
        emissorImg.setVisible(true);
        remetenteImg.setVisible(true);
        emissorReceptor.setVisible(true);

        int ttl = 5; // valor de TTL inicial
        sendPacketsWithTTL(ttl);
    }

    @FXML
    private void clicouInicio(MouseEvent event) {
        resetScreen();
    }

    // metodo que reseta os componentes visuais necessarios ao clicar em restart
    private void resetScreen() {
        // limpa nos e arestas, mantendo componentes essenciais como o botao de reset
        mainPane.getChildren()
                .removeIf(node -> node instanceof ImageView && node != backgroundImageView && node != botaoInicio);

        // remove as arestas
        edges.clear();
        mainPane.getChildren().removeIf(node -> node instanceof Line);

        // limpa o contador de pacotes
        totalPackets.set(0);
        packetCounterText.setText("0");

        // retorna os nos selecionados para roteadores padroes
        if (emissorNode != null) {
            emissorNode.setImage(new Image("file:./assets/nó.png"));
            emissorNode = null;
        }
        if (remetenteNode != null) {
            remetenteNode.setImage(new Image("file:./assets/nó.png"));
            remetenteNode = null;
        }

        // configurando elementos essenciais
        if (backgroundImageView == null) {
            Image backgroundImage = new Image("file:./assets/backgroundImg.png");
            backgroundImageView = new ImageView(backgroundImage);
            backgroundImageView.setFitWidth(mainPane.getWidth());
            backgroundImageView.setFitHeight(mainPane.getHeight());
            mainPane.getChildren().add(backgroundImageView);
        }

        // readicionando componentes essenciais
        if (!mainPane.getChildren().contains(titulo)) {
            mainPane.getChildren().add(titulo);
        }
        if (!mainPane.getChildren().contains(iniciar)) {
            mainPane.getChildren().add(iniciar);
        }
        if (!mainPane.getChildren().contains(botaoInicio)) {
            mainPane.getChildren().add(botaoInicio);
        }
        if (!mainPane.getChildren().contains(spongeUp)) {
            mainPane.getChildren().add(spongeUp);
        }

        if (!mainPane.getChildren().contains(emissorReceptor)) {
            mainPane.getChildren().add(emissorReceptor);
        }

        // reconfigurando visibilidade
        iniciar.setVisible(true);
        titulo.setVisible(true);
        botaoInicio.setVisible(true);
        spongeUp.setVisible(true);
        emissorReceptor.setVisible(false);

        packetCounterText.setVisible(false);

        // define emissor e remetente com img padrao
        if (emissorImg != null) {
            emissorImg.setImage(new Image("file:./assets/emissor.png"));
        }
        if (remetenteImg != null) {
            remetenteImg.setImage(new Image("file:./assets/remetente.png"));
        }

        // garante que a tela estara limpa
        mainPane.requestLayout();
    }

    // metodo utilizado pelas opcoes 3 e 4, implementa a logica do TTL
    private void sendPacketsWithTTL(int ttl) {
        if (emissorNode == null) {
            return;
        }

        int emissorId = getNodeId(emissorNode);

        for (int i = 1; i <= nodes.size(); i++) {
            if (i != emissorId) {
                Line edge = edges.get(emissorId + "-" + i);
                ImageView destinationNode = nodes.get(i);

                if (edge != null && destinationNode != null) {
                    Packet packet = new Packet(emissorNode, destinationNode, edge, this, ttl);
                    packet.start(); // Inicia a thread para o pacote com TTL
                }
            }
        }
    }

    public void buttonOff() {
    }

    // metodo initialize necessario para iniciar variaveis e componentes visuais
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        packetCounterText.setFont(Font.font("Impact", FontWeight.BOLD, 25));
        packetCounterText.setFill(Color.YELLOW); // Define a cor do texto para amarelo
        packetCounterText.setStroke(Color.BLACK); // Define a cor do contorno para preto
        packetCounterText.setStrokeWidth(2); // Define a largura do contorno
        emissorImg.setVisible(false);
        remetenteImg.setVisible(false);
        createdPacks.setVisible(false);
        packetCounterText.setVisible(false);
        emissorReceptor.setVisible(false);
        if (packetCounterText == null) {
        } else {
            packetCounterText.setText("0"); // Teste de inicialização
        }
        ColorAdjust colorAdjust = new ColorAdjust();
        colorAdjust.setBrightness(-0.5);

        iniciar.setOnMouseEntered(event -> {
            iniciar.setEffect(colorAdjust);
        });

        iniciar.setOnMouseExited(event -> {
            iniciar.setEffect(null);
        });

        if (!mainPane.getChildren().contains(botaoInicio)) {
            mainPane.getChildren().add(botaoInicio);
        }

        botaoInicio.setOnMouseEntered(event -> {
            botaoInicio.setEffect(colorAdjust);
        });

        botaoInicio.setOnMouseExited(event -> {
            botaoInicio.setEffect(null);
        });

        // Inicializa emissor e remetente com imagens padrão
        emissorImg.setImage(new Image("file:./assets/emissor.png"));
        remetenteImg.setImage(new Image("file:./assets/remetente.png"));

        // Adiciona createdPacks se não estiver presente
        if (!mainPane.getChildren().contains(createdPacks)) {
            mainPane.getChildren().add(createdPacks);
        }
    }

    // metodo utilizado para tocar musica
    public void playMusic() throws UnsupportedAudioFileException, IOException, LineUnavailableException {
        File file = new File("misc/temaBobEsponja.wav");
        AudioInputStream audioStream = AudioSystem.getAudioInputStream(file);
        Clip clip = AudioSystem.getClip();
        clip.open(audioStream);
        FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        gainControl.setValue(-20.0f);
        clip.start();
        clip.loop(Clip.LOOP_CONTINUOUSLY);
    }
}