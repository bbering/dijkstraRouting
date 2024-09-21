
/* ***************************************************************
* Autor............: Breno Bering Silva
* Matricula........: 202110863
* Inicio...........: 19/09/2024
* Ultima alteracao.: 21/09/2024
* Nome.............: Principal
* Funcao...........: .java que inicia a cena principal
****************************************************************/
import control.mainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Principal extends Application {

  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage primaryStage) throws Exception {
    // carregando o FXML
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/assets/background.fxml"));

    // instanciando o root
    Parent root = loader.load();

    // setando controlador e iniciando musica
    mainController controller = loader.getController();
    controller.playMusic();

    // configurando cena e proporcoes
    Scene scene = new Scene(root);
    primaryStage.setScene(scene);
    primaryStage.setWidth(900);
    primaryStage.setHeight(700);
    primaryStage.setResizable(false);
    primaryStage.setTitle("Roteamento Dijkstra");
    primaryStage.show();
  }
}