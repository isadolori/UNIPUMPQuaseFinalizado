package com.example.unipump

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore

class TelaInformacoesPessoaisAluno : AppCompatActivity() {

    private lateinit var nome_usuario: EditText
    private lateinit var endereco: EditText
    private lateinit var genero: EditText
    private lateinit var nome: TextView
    private lateinit var sobre_nome: TextView
    private lateinit var numero_contato: EditText
    private lateinit var idade: TextView

    private val db = FirebaseFirestore.getInstance()

    private lateinit var btn_voltar: ImageView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_tela_informacoes_pessoais_aluno)


        // IDs
        btn_voltar = findViewById(R.id.btn_back)
        nome_usuario = findViewById(R.id.nome_usuario)
        endereco = findViewById(R.id.endereco_usuario)
        genero = findViewById(R.id.genero_usuario)
        nome = findViewById(R.id.primeiro_nome_usuario)
        sobre_nome = findViewById(R.id.sobrenome_usuario)
        numero_contato = findViewById(R.id.numero_contato_usuario)
        idade = findViewById(R.id.idade_usuario)


        buscarDadosAluno("UjJe5GoXPQeqhmwiAiib")

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> {
                    val intent = Intent(this, TelaPrincipalAluno::class.java)
                    startActivity(intent)
                    true
                }

                R.id.nav_treinos -> {
                    val intent = Intent(this, TelaTreinoAluno::class.java)
                    startActivity(intent)
                    true
                }

                R.id.nav_config -> {
                    true
                }

                R.id.nav_chat -> {
                    val intent = Intent(this,TelaChat::class.java)
                    startActivity(intent)
                    true
                }

                else -> false
            }
        }



    }

    private fun buscarDadosAluno(documentId: String){
        db.collection("alunos").document(documentId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()){
                    nome_usuario.setText(document.getString("nome_usuario") ?: "")
                    endereco.setText(document.getString("endereco") ?: "")
                    genero.setText(document.getString("genero") ?: "")
                    numero_contato.setText(document.getString("telefone") ?: "")
                    nome.text = document.getString("nome") ?: ""
                    sobre_nome.text = document.getString("genero") ?: ""
                    idade.text = document.getString("idade") ?: ""

                }

            }
    }

    private fun atualizarDados(documentId: String, campo: String, valor: String){
        db.collection("alunos").document(documentId)
            .update()
    }

    override fun onStart() {

        // Voltar
        btn_voltar.setOnClickListener{
            val itent = Intent(this, TelaConfig:: class.java)
            startActivity(itent) //Tela de configurações
        }


        super.onStart()
        Log.d("CicloDeVida", "onStart chamado")
    }

    override fun onResume() {
        super.onResume()
        Log.d("CicloDeVida", "onResume chamado")
    }

    override fun onPause() {
        super.onPause()
        Log.d("CicloDeVida", "onPause chamado")
    }

    override fun onStop() {
        super.onStop()
        Log.d("CicloDeVida", "onStop chamado")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("CicloDeVida", "onDestroy chamado")
    }
}