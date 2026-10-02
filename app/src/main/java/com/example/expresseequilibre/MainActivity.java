package com.example.expresseequilibre;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Création de la vue personnalisée du jeu.
        gameView = new GameView(this);

        // Affichage de GameView comme écran principal de l'application.
        setContentView(gameView);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // L'application revient au premier plan :
        // la boucle de jeu peut tourner.
        if (gameView != null) {
            gameView.startGame();
        }
    }

    @Override
    protected void onPause() {
        // L'application perd le focus :
        // arrêt de la boucle pour ne pas consommer de ressources inutilement.
        if (gameView != null) {
            gameView.stopGame();
        }

        super.onPause();
    }
}