package com.example.unipump

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.w3c.dom.Text

class TelaInformacaoPessoal_funcionario : AppCompatActivity() {

    private lateinit var btnSetaVoltar : ImageButton
    private lateinit var btnNavegacao : BottomNavigationView

    private lateinit var campoNomeUsuario: EditText
    private lateinit var campoEnderecoUsuario: EditText
    private lateinit var generoUsuario: EditText
    private lateinit var numeroContato: EditText

    private lateinit var primeiro_nome_usuario: TextView
    private lateinit var sobrenome_usuario: TextView
    private lateinit var campo_idade_usuario: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_tela_informacao_pessoal_funcionario)

        btnSetaVoltar = findViewById(R.id.SetaVoltarTelaGerenciamentoAluno)
        btnNavegacao = findViewById(R.id.bottom_navigation)



        campoNomeUsuario = findViewById(R.id.campo_nome_usuario)
        campoEnderecoUsuario  = findViewById(R.id.campo_endereco_usuario)
        generoUsuario = findViewById(R.id.genero)
        numeroContato = findViewById(R.id.numero_contato)
        primeiro_nome_usuario = findViewById(R.id.primeiro_nome_usuario)
        sobrenome_usuario = findViewById(R.id.sobrenome_usuario)
        campo_idade_usuario = findViewById(R.id.campo_idade_usuario)


        val prefs = getSharedPreferences("funcionarioPrefs", MODE_PRIVATE)
        val nomeUsuario = prefs.getString("nome_usuario", "")
        val enderecoUsuario = prefs.getString("endereco", "")
        val generoUsuario1 = prefs.getString("genero", "")
        val numeroContato1 = prefs.getString("telefone", "")
        val nome = prefs.getString("nome", "")
        val sobrenomeUsuario = prefs.getString("sobrenome", "")
        val idadeUsuario = prefs.getString("idade", "")

        campoNomeUsuario.setText(nomeUsuario)
        campoEnderecoUsuario.setText(enderecoUsuario)
        generoUsuario.setText(generoUsuario1)
        numeroContato.setText(numeroContato1)
        primeiro_nome_usuario.text = nome
        sobrenome_usuario.text = sobrenomeUsuario
        campo_idade_usuario.text = idadeUsuario




        configurarEventos()

    }

    fun configurarEventos(){

        // Definindo o clique do botão de voltar
        btnSetaVoltar.setOnClickListener {
            finish() // Isso chama o comportamento de voltar para a tela anterior
        }


        btnNavegacao.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> {
                    // O que acontece quando o item "Início" é clicado
                    val intent = Intent(this, TelaFuncionario::class.java)
                    startActivity(intent)
                    true
                }

                R.id.nav_chat -> {
                    // Abre a tela de chat
                    val intent = Intent(this, TelaChat::class.java)
                    startActivity(intent)
                    true
                }

                R.id.nav_config -> {
                    // Abre a tela de configurações
                    val intent = Intent(this, TelaConfiguracao_Funcionario::class.java)
                    startActivity(intent)
                    true
                }

                else -> false
            }
        }






    }

    override fun onStart() {
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