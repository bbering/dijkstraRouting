package control;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
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
    private Text routeCost;

    @FXML
    private ImageView routeImgCost;

    @FXML
    private ImageView emissorImg;

    @FXML
    private ImageView remetenteImg;

    @FXML
    private ImageView spongeUp;

    int iniciouDjikstra = 0; // opcao padrao

    // utilizado para garantir a sincronizacao entre as threads
    private AtomicInteger custoCaminho = new AtomicInteger(0);

    private Map<Integer, ImageView> nodes = new HashMap<>();
    private Map<String, Line> edges = new HashMap<>();
    private ImageView backgroundImageView; // referencia do backgroundImg

    private ImageView emissorNode = null; // no emissor
    private ImageView remetenteNode = null; // no receptor

    private Map<String, Integer> edgeWeights = new HashMap<>(); // valor das arestas
    private Map<String, Label> edgeWeightLabels = new HashMap<>(); // valores das arestas em string

    // metodo principal utilizado para gerar o grafo na tela
    private void createAndShowGraph(int numberOfNodes) {

        // os metodos abaixo servem para caso os recursos visuais sejam apagados em
        // algum momento, eles sejam
        // readicionados no mainPane

        Platform.runLater(() -> {
            if (!mainPane.getChildren().contains(botaoInicio)) {
                mainPane.getChildren().add(botaoInicio);
            }
        });

        Platform.runLater(() -> {
            if (!mainPane.getChildren().contains(routeCost)) {
                mainPane.getChildren().add(routeCost);
            }
        });

        mainPane.getChildren()
                .removeIf(node -> (node instanceof ImageView && node != backgroundImageView && node != routeImgCost)
                        || node == routeCost);

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

        double radius = 200; // Raio do hexágono
        double centerX = 450; // Centro do hexágono em X
        double centerY = 330; // Centro do hexágono em Y
        double nodeSize = 100; // Tamanho dos nós (largura e altura)

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

            // caso createdPacks foi removido
            if (!mainPane.getChildren().contains(routeImgCost)) {
                mainPane.getChildren().add(routeImgCost);
            }

            nodes.put(i, nodeImageView);

            // evento de clique necessario para escolher no emissor e receptor
            nodeImageView.setOnMouseClicked(event -> nodeClicked(nodeImageView));
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

    private void showEdgeWeights(int emissorId, int receptorId) {
        for (String key : edgeWeightLabels.keySet()) {
            Label weightLabel = edgeWeightLabels.get(key);
            System.out.println("Checando chave: " + key); // Adicione este log
            weightLabel.setVisible(true);
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

            double midX = (edge.getStartX() + edge.getEndX()) / 2;
            double midY = (edge.getStartY() + edge.getEndY()) / 2;

            Label weightLabel = new Label(String.valueOf(weight));
            weightLabel.setStyle(
                    "-fx-background-color: white; -fx-border-color: black; -fx-padding: 2px; -fx-font-size: 14px;");
            weightLabel.setPrefWidth(30);
            weightLabel.setAlignment(Pos.CENTER);
            weightLabel.setVisible(false);

            Platform.runLater(() -> {
                if (!mainPane.getChildren().contains(weightLabel)) {
                    mainPane.getChildren().add(weightLabel);
                }
            });

            double deltaX = edge.getEndX() - edge.getStartX();
            double deltaY = edge.getEndY() - edge.getStartY();
            double angle = Math.toDegrees(Math.atan2(deltaY, deltaX));

            if (angle > 90 || angle < -90) {
                angle += 180;
            }

            weightLabel.setRotate(angle);
            weightLabel.setLayoutX(midX - weightLabel.getPrefWidth() / 2);
            weightLabel.setLayoutY(midY - weightLabel.getHeight() / 2);

            double labelOffset = 10;
            double offsetX = labelOffset * Math.sin(Math.toRadians(angle));
            double offsetY = labelOffset * -Math.cos(Math.toRadians(angle));

            weightLabel.setLayoutX(weightLabel.getLayoutX() + offsetX);
            weightLabel.setLayoutY(weightLabel.getLayoutY() + offsetY);

            edges.put(node1 + "-" + node2, edge);
            edges.put(node2 + "-" + node1, edge);
            edgeWeights.put(node1 + "-" + node2, weight);
            edgeWeights.put(node2 + "-" + node1, weight);

            edgeWeightLabels.put(node1 + "-" + node2, weightLabel);
            edgeWeightLabels.put(node2 + "-" + node1, weightLabel);

            Platform.runLater(() -> {
                if (!mainPane.getChildren().contains(edge)) {
                    mainPane.getChildren().add(edge);
                }
            });
        }
    }

    // metodo que esconde as labels de peso ate o usuario escolher novos nos emissor
    // e receptor
    private void hideEdgeWeights() {
        for (Map.Entry<String, Label> entry : edgeWeightLabels.entrySet()) {
            Label weightLabel = entry.getValue();
            weightLabel.setVisible(false);
        }
    }

    // metodo que implementa a logica do usuario clicar em um no para escolhe-lo
    // como emissor ou remetente
    private void nodeClicked(ImageView nodeImageView) {
        if (emissorNode == null) {
            emissorNode = nodeImageView;
            emissorNode.setImage(new Image("file:./assets/emissor.png"));
        } else if (remetenteNode == null && nodeImageView != emissorNode) {
            remetenteNode = nodeImageView;
            remetenteNode.setImage(new Image("file:./assets/remetente.png"));

            if (iniciouDjikstra == 1) {
                emissorReceptor.setVisible(false);
                Integer emissorId = getNodeId(emissorNode);
                Integer receptorId = getNodeId(remetenteNode);
                dijkstraRouting(emissorId, receptorId);
                showEdgeWeights(emissorId, receptorId); // Mostra pesos apenas após seleção
            }
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

    private Integer getNodeId(ImageView node) {
        return nodes.entrySet()
                .stream()
                .filter(entry -> entry.getValue().equals(node))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
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
        routeCost.setVisible(true);
        routeImgCost.setVisible(true);
        emissorImg.setVisible(true);
        remetenteImg.setVisible(true);
        emissorReceptor.setVisible(true);
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

        // remove as arestas e os valores
        edges.clear();

        hideEdgeWeights();

        mainPane.getChildren().removeIf(node -> node instanceof Line);

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

        // limpando o custo total do caminho
        custoCaminho.set(0);
        routeCost.setText("0");

        // reconfigurando visibilidade
        iniciar.setVisible(true);
        titulo.setVisible(true);
        botaoInicio.setVisible(true);
        spongeUp.setVisible(true);
        emissorReceptor.setVisible(false);
        routeImgCost.setVisible(false);
        routeCost.setVisible(false);

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

    public void buttonOff() {
    }

    // algoritmo de dijkstra
    public void dijkstraRouting(int startNode, int endNode) {
        new Thread(() -> {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
            }
            // Mapa para armazenar a distância mínima de cada nó ao nó inicial
            Map<Integer, Integer> distances = new HashMap<>();
            // Mapa para armazenar o nó anterior no caminho mais curto
            Map<Integer, Integer> previousNodes = new HashMap<>();
            // Conjunto de nós não visitados
            Set<Integer> unvisitedNodes = new HashSet<>(nodes.keySet());

            // Inicializa as distâncias
            for (Integer node : nodes.keySet()) {
                distances.put(node, Integer.MAX_VALUE);
                previousNodes.put(node, null);
            }
            distances.put(startNode, 0);

            while (!unvisitedNodes.isEmpty()) {
                int currentNode = Collections.min(unvisitedNodes,
                        (node1, node2) -> Integer.compare(distances.get(node1), distances.get(node2)));

                if (currentNode == endNode) {
                    break;
                }

                unvisitedNodes.remove(currentNode);

                for (Integer neighbor : getAdjacentNodes(currentNode)) {
                    String edgeKey = currentNode + "-" + neighbor;
                    if (!edgeWeights.containsKey(edgeKey)) {
                        continue;
                    }
                    int edgeWeight = edgeWeights.get(edgeKey);
                    int altDistance = distances.get(currentNode) + edgeWeight;

                    if (altDistance < distances.get(neighbor)) {
                        distances.put(neighbor, altDistance);
                        previousNodes.put(neighbor, currentNode);
                    }
                }

                highlightShortestPath(Collections.singletonList(currentNode));
            }

            List<Integer> path = new ArrayList<>();
            for (Integer node = endNode; node != null; node = previousNodes.get(node)) {
                path.add(node);
            }
            Collections.reverse(path);

            // Atualiza o custo total e exibe na tela
            int totalCost = 0; // Inicializa o totalCost
            for (int i = 0; i < path.size() - 1; i++) {
                String edgeKey = path.get(i) + "-" + path.get(i + 1);
                if (edgeWeights.containsKey(edgeKey)) {
                    int edgeWeight = edgeWeights.get(edgeKey);
                    totalCost += edgeWeight; // Incrementa o custo total

                    // Atualiza a interface gráfica e muda a cor da aresta
                    final int costToDisplay = totalCost;
                    Platform.runLater(() -> {
                        routeCost.setText(String.valueOf(costToDisplay));
                        Line edge = edges.get(edgeKey);
                        if (edge != null) {
                            edge.setStroke(Color.RED);
                            edge.setStrokeWidth(3);
                        }
                    });

                    // Aguarda 2 segundos antes de prosseguir para a próxima aresta
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }).start();
    }

    // Método para destacar o caminho mais curto
    private void highlightShortestPath(List<Integer> path) {
        for (int i = 0; i < path.size() - 1; i++) {
            String edgeKey = path.get(i) + "-" + path.get(i + 1);
            Line edge = edges.get(edgeKey);
            if (edge != null) {
                edge.setStroke(Color.RED); // muda a cor da aresta para vermelho
                edge.setStrokeWidth(3); // aumenta a largura da aresta
            }
        }
    }

    // Método auxiliar para obter os nós adjacentes
    private List<Integer> getAdjacentNodes(int node) {
        return edges.keySet().stream()
                .filter(key -> key.startsWith(node + "-") || key.endsWith("-" + node))
                .map(key -> {
                    String[] nodes = key.split("-");
                    return Integer.parseInt(nodes[0].equals(String.valueOf(node)) ? nodes[1] : nodes[0]);
                })
                .collect(Collectors.toList());
    }

    // metodo initialize necessario para iniciar variaveis e componentes visuais
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        routeCost.setFont(Font.font("Impact", FontWeight.BOLD, 25));
        routeCost.setFill(Color.YELLOW); // Define a cor do texto para amarelo
        routeCost.setStroke(Color.BLACK); // Define a cor do contorno para preto
        routeCost.setStrokeWidth(2); // Define a largura do contorno
        emissorImg.setVisible(false);
        remetenteImg.setVisible(false);
        emissorReceptor.setVisible(false);
        ColorAdjust colorAdjust = new ColorAdjust();
        colorAdjust.setBrightness(-0.5);
        routeCost.setVisible(false);
        routeImgCost.setVisible(false);

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

        // Adiciona createdPacks se não estiver presente
        if (!mainPane.getChildren().contains(routeImgCost)) {
            mainPane.getChildren().add(routeImgCost);
        }

        // Inicializa emissor e remetente com imagens padrão
        emissorImg.setImage(new Image("file:./assets/emissor.png"));
        remetenteImg.setImage(new Image("file:./assets/remetente.png"));

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