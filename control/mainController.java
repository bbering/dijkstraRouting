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
import javafx.scene.effect.DropShadow;
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

    // recursos visuais

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

    private List<Text> letterTexts = new ArrayList<>(); // lista onde as letras dos roteadores sao armazenados

    private Map<Integer, ImageView> nodes = new HashMap<>(); // lista contendo nos
    private Map<String, Line> edges = new HashMap<>(); // lista contendo arestas
    private ImageView backgroundImageView; // referencia do backgroundImg

    private ImageView emissorNode = null; // no emissor
    private ImageView remetenteNode = null; // no receptor

    private Map<String, Integer> edgeWeights = new HashMap<>(); // valor das arestas
    private Map<String, Label> edgeWeightLabels = new HashMap<>(); // valores das arestas em string

    // metodo principal utilizado para gerar o grafo na tela
    private void createAndShowGraph(int numberOfNodes) {
        // Adiciona o botão Iniciar ao mainPane se ainda não estiver presente
        Platform.runLater(() -> {
            if (!mainPane.getChildren().contains(botaoInicio)) {
                mainPane.getChildren().add(botaoInicio);
            }
        });

        // Adiciona a rota de custo ao mainPane se ainda não estiver presente
        Platform.runLater(() -> {
            if (!mainPane.getChildren().contains(routeCost)) {
                mainPane.getChildren().add(routeCost);
            }
        });

        // Limpa os nós, mas não remove elementos essenciais
        mainPane.getChildren()
                .removeIf(node -> (node instanceof ImageView && node != backgroundImageView && node != routeImgCost) ||
                        node == routeCost);

        // Adiciona o emissor receptor ao mainPane se ainda não estiver presente
        Platform.runLater(() -> {
            if (!mainPane.getChildren().contains(emissorReceptor)) {
                mainPane.getChildren().add(emissorReceptor);
            }
        });

        // Limpa os mapas de nós e arestas
        nodes.clear();
        edges.clear();

        // Adiciona o backgroundImageView se ainda não estiver presente
        if (!mainPane.getChildren().contains(backgroundImageView)) {
            if (backgroundImageView == null) {
                Image backgroundImage = new Image("file:./assets/backgroundImg.png");
                backgroundImageView = new ImageView(backgroundImage);
                backgroundImageView.setFitWidth(mainPane.getWidth());
                backgroundImageView.setFitHeight(mainPane.getHeight());
            }
            mainPane.getChildren().add(backgroundImageView);
        }

        double radius = 200;
        double centerX = 450;
        double centerY = 330;
        double nodeSize = 145;

        for (int i = 1; i <= numberOfNodes; i++) {
            Image nodeImage;
            try {
                nodeImage = new Image(getClass().getResourceAsStream("/assets/no.png"));
            } catch (Exception e) {
                System.err.println("Erro ao carregar a imagem do nó: " + e.getMessage());
                continue;
            }

            ImageView nodeImageView = new ImageView(nodeImage);
            nodeImageView.setId("node" + i);
            nodeImageView.setFitWidth(nodeSize);
            nodeImageView.setFitHeight(nodeSize);

            double angle = 2 * Math.PI * i / numberOfNodes;
            double x = centerX + radius * Math.cos(angle) - nodeSize / 2;
            double y = centerY + radius * Math.sin(angle) - nodeSize / 2;

            nodeImageView.setLayoutX(x);
            nodeImageView.setLayoutY(y);

            char nodeLetter = (char) ('A' + (i - 1));
            Text letterText = new Text(String.valueOf(nodeLetter));
            letterText.setFont(Font.font("Impact", FontWeight.BOLD, 32));
            letterText.setFill(Color.YELLOW);
            letterText.setLayoutX(x + nodeSize / 2 - 10);
            letterText.setLayoutY(y + nodeSize / 2 + 10);
            letterText.setStroke(Color.BLACK);
            letterText.setStrokeWidth(2);

            letterTexts.add(letterText);

            // Adiciona os nós e letras ao mainPane
            mainPane.getChildren().addAll(nodeImageView, letterText);
            nodes.put(i, nodeImageView);

            // Configura o evento de clique
            nodeImageView.setOnMouseClicked(event -> nodeClicked(nodeImageView));
        }

        // Verifica se routeImgCost está presente
        if (!mainPane.getChildren().contains(routeImgCost)) {
            mainPane.getChildren().add(routeImgCost);
        }
    }

    // metodo para limpar as letras
    public void clearLetterTexts() {
        // remove todas as letras do mainPane
        for (Text text : letterTexts) {
            mainPane.getChildren().remove(text);
        }
        letterTexts.clear(); // limpa a lista
    }

    // metodo utilizado para criar arestas entre os nos
    public void createEdges() {
        for (Line edge : edges.values()) {
            if (!mainPane.getChildren().contains(edge)) {
                mainPane.getChildren().add(edge);
            }
        }
    }

    // metodo usado para mostrar o peso das arestas
    private void showEdgeWeights(int emissorId, int receptorId) {
        for (String key : edgeWeightLabels.keySet()) {
            Label weightLabel = edgeWeightLabels.get(key);
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

                    // limpa e prepara os nós e arestas antes de criar um novo gráfico
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

                    // mostra as arestas apos criacao dos nos
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
            edge.setStroke(Color.WHITE);
            edge.setStrokeWidth(3);

            double midX = (edge.getStartX() + edge.getEndX()) / 2;
            double midY = (edge.getStartY() + edge.getEndY()) / 2;

            Label weightLabel = new Label(String.valueOf(weight));
            weightLabel.setStyle(
                    "-fx-font-family: Impact; -fx-font-size: 24px; -fx-text-fill: yellow;"); // cor do texto
            weightLabel.setPrefWidth(30);
            weightLabel.setAlignment(Pos.CENTER);
            weightLabel.setVisible(false);

            // sombreamento na cor preta dos pesos
            DropShadow dropShadow = new DropShadow();
            dropShadow.setColor(Color.BLACK);
            dropShadow.setRadius(0);
            dropShadow.setSpread(1);
            dropShadow.setWidth(4.5);
            dropShadow.setHeight(4.5);
            weightLabel.setEffect(dropShadow);

            Platform.runLater(() -> {
                if (!mainPane.getChildren().contains(weightLabel)) {
                    mainPane.getChildren().add(weightLabel);
                }
            });

            // orientacao da weightLabel
            weightLabel.setRotate(360);

            // centralizando o label no meio da aresta
            weightLabel.setLayoutX(midX - weightLabel.getPrefWidth() / 2);
            weightLabel.setLayoutY(midY - weightLabel.getHeight() / 2);

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
        return iniciouDjikstra; // getter metodo usado para testes
    }

    public void setIniciouDjikstra(int iniciouDjikstra) {
        this.iniciouDjikstra = iniciouDjikstra;
    }

    // metodo que retorna o id do no dentro da lista

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
        return mainPane; // retorna a pane principal
    }

    public ImageView getRemetenteNode() {
        return remetenteNode; // retorna a imgView do remetente
    }

    // metodos que implementam a logica dos botoes

    @FXML
    void clicouIniciar(MouseEvent event) {
        titulo.setVisible(false);
        setIniciouDjikstra(1);
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
            emissorNode.setImage(new Image(getClass().getResourceAsStream("/assets/no.png")));
            emissorNode = null;
        }
        if (remetenteNode != null) {
            remetenteNode.setImage(new Image(getClass().getResourceAsStream("/assets/no.png")));
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

        routeCost.setText("0");
        clearLetterTexts();

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

    // algoritmo de dijkstra
    public void dijkstraRouting(int startNode, int endNode) {
        new Thread(() -> {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
            }
            // mapa que armazena distancias entre nos
            Map<Integer, Integer> distances = new HashMap<>();
            // mapa que armazena os nos anteriores ao longo do algoritmo
            Map<Integer, Integer> previousNodes = new HashMap<>();
            // nos nao visitados (quando a lista esta vazia e nao ha mais nos nao visitados,
            // encerra o algoritmo)
            Set<Integer> unvisitedNodes = new HashSet<>(nodes.keySet());

            // inicializando distancias
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

            // atualizando o custo total para exibi-lo na tela
            int totalCost = 0;
            for (int i = 0; i < path.size() - 1; i++) {
                String edgeKey = path.get(i) + "-" + path.get(i + 1);
                if (edgeWeights.containsKey(edgeKey)) {
                    int edgeWeight = edgeWeights.get(edgeKey);
                    totalCost += edgeWeight; // incrementa o custo total

                    // atualiza a interface e muda cor da aresta do caminho mais curto
                    final int costToDisplay = totalCost;
                    Platform.runLater(() -> {
                        routeCost.setText(String.valueOf(costToDisplay));
                        Line edge = edges.get(edgeKey);
                        if (edge != null) {
                            edge.setStroke(Color.ORANGERED);
                            edge.setStrokeWidth(3);
                        }
                    });

                    // aguarda 2 segundos para melhor visualizacao da animacao

                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }).start();
    }

    // metodo que destaca o caminho mais curto

    private void highlightShortestPath(List<Integer> path) {
        for (int i = 0; i < path.size() - 1; i++) {
            String edgeKey = path.get(i) + "-" + path.get(i + 1);
            Line edge = edges.get(edgeKey);
            if (edge != null) {
                edge.setStroke(Color.ORANGERED); // muda a cor da aresta para vermelho
                edge.setStrokeWidth(3); // aumenta a largura da aresta
            }
        }
    }

    // metodo para obter os nos adjascentes auxiliar do algoritmo de dijkstra

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
        routeCost.setFill(Color.YELLOW);
        routeCost.setStroke(Color.BLACK);
        routeCost.setStrokeWidth(2);
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

        // adiciona a imageview de custo caso tenha sido removida
        if (!mainPane.getChildren().contains(routeImgCost)) {
            mainPane.getChildren().add(routeImgCost);
        }

        // inicializa emissor e remetente com img padrao
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