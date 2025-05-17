package com.example.unipump

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore

class TelaFichaTreino : AppCompatActivity() {

    private lateinit var tituloExercicio: TextView
    private val db = FirebaseFirestore.getInstance()
    private lateinit var bntComecar: Button
    private lateinit var btnVoltar: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_tela_ficha_treino)

        bntComecar = findViewById(R.id.buttonStart)
        btnVoltar = findViewById(R.id.btn_back)

        configurarEvento()


        // 1. Acessar o BottomNavigationView
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)


        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> {
                    // Já está na tela de Início, talvez não precise fazer nada
                    true
                }

                R.id.nav_treinos -> {
                    // Ir para a tela de treinos
                    val intent = Intent(this, TelaTreinoAluno::class.java)
                    startActivity(intent)
                    true
                }

                R.id.nav_config -> {
                    val intent = Intent(this, TelaConfig::class.java)
                    startActivity(intent)
                    true

                }

                R.id.nav_chat -> {
                    val intent = Intent(this,TelaChat::class.java)
                    startActivity(intent)
                    true
                }

                else -> {
                    false
                }
            }

        }

        // Título
        tituloExercicio = findViewById(R.id.Titulo_exercicio)

        val funcionarioId = "2413103"
        val caminhoColecao = "funcionarios/$funcionarioId/treinos"

        db.collection(caminhoColecao)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents){
                    val treino1 = document.getString("treino1")
                    val treino2 = document.getString("treino2")


                    tituloExercicio.text = "$treino1\n$treino2"
                    break
                }

            }
    }



    private fun configurarEvento(){

        bntComecar.setOnClickListener {
            val intent = Intent(this, TelaExercicioFinalizado:: class.java)
            startActivity(intent)
        }

        btnVoltar.setOnClickListener {
            finish()
        }


    }

}