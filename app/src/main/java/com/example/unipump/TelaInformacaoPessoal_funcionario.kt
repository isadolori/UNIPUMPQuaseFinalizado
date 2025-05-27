package com.example.unipump

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore

class TelaInformacaoPessoal_funcionario : AppCompatActivity() {

    // Views
    private lateinit var nomeUsuarioEt: EditText
    private lateinit var enderecoEt: EditText
    private lateinit var generoEt: EditText
    private lateinit var telefoneEt: EditText
    private lateinit var primeiroNomeTv: TextView
    private lateinit var sobrenomeTv: TextView
    private lateinit var idadeTv: TextView
    private lateinit var btnVoltar: ImageView
    private lateinit var bottomNav: BottomNavigationView

    // Firestore
    private val db by lazy {
        FirebaseFirestore.getInstance()
    }

    // SharedPreferences onde guardamos o funcionarioDocId no login
    private val prefs by lazy {
        getSharedPreferences("funcionarioPrefs", MODE_PRIVATE)
    }

    // recupera o ID de documento Firestore do funcionário
    private val funcionarioDocId: String?
        get() = prefs.getString("usuarioDocId", null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tela_informacao_pessoal_funcionario)

        // 1) Bind das views
        nomeUsuarioEt = findViewById(R.id.campo_nome_usuario)
        enderecoEt = findViewById(R.id.campo_endereco_usuario)
        generoEt = findViewById(R.id.genero)
        telefoneEt = findViewById(R.id.numero_contato)
        primeiroNomeTv = findViewById(R.id.primeiro_nome_usuario)
        sobrenomeTv = findViewById(R.id.sobrenome_usuario)
        idadeTv = findViewById(R.id.campo_idade_usuario)
        btnVoltar = findViewById(R.id.SetaVoltarTelaGerenciamentoAluno)
        bottomNav = findViewById(R.id.bottom_navigation)

        // 2) Configura botão de voltar
        btnVoltar.setOnClickListener {
            salvarDados()
            finish()
        }

        // 3) Bottom navigation
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> {
                    startActivity(Intent(this, TelaFuncionario::class.java))
                    salvarDados()
                    true
                }
                R.id.nav_chat -> {
                    startActivity(Intent(this, TelaChat::class.java))
                    salvarDados()
                    true
                }
                R.id.nav_config -> {
                    startActivity(Intent(this, TelaConfiguracao_Funcionario::class.java))
                    salvarDados()
                    true
                }
                else -> false
            }
        }

        // 4) Puxa o funcionarioDocId e busca dados
        funcionarioDocId?.let { docId ->
            buscarDadosFuncionario(docId)
        } ?: run {
            Toast.makeText(
                this,
                "Não encontrei seu perfil. Faça login novamente.",
                Toast.LENGTH_LONG
            ).show()
            startActivity(Intent(this, TelaLogin::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("TelaInfoFuncionario", "onResume chamado")
        // recarrega sempre que a Activity volta ao foco
        funcionarioDocId?.let { buscarDadosFuncionario(it) }
    }

    override fun onBackPressed() {
        // garante que salve antes de sair
        salvarDados()
        super.onBackPressed()
    }

    private fun buscarDadosFuncionario(docId: String) {
        Log.d("TelaInfoFuncionario", "Buscando dados em funcionarios/$docId")
        db.collection("funcionarios").document(docId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    nomeUsuarioEt.setText(doc.getString("nome_usuario"))
                    enderecoEt.setText(doc.getString("endereco"))
                    generoEt.setText(doc.getString("genero"))
                    telefoneEt.setText(doc.getString("telefone"))
                    primeiroNomeTv.text = doc.getString("nome") ?: ""
                    sobrenomeTv.text = doc.getString("sobrenome") ?: ""
                    idadeTv.text = doc.getString("idade") ?: ""


                } else {
                    Log.w("TelaInfoFuncionario", "Documento não existe.")
                }
            }
            .addOnFailureListener { e ->
                Log.e("TelaInfoFuncionario", "Erro ao buscar dados", e)
                Toast.makeText(this, "Falha ao carregar seus dados.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun salvarDados() {
        val docId = funcionarioDocId ?: return
        val mapa = mapOf(
            "nome_usuario" to nomeUsuarioEt.text.toString(),
            "endereco" to enderecoEt.text.toString(),
            "genero" to generoEt.text.toString(),
            "telefone" to telefoneEt.text.toString()
        )

        // Atualiza SharedPreferences local
        val edit = prefs.edit()
        edit.putString("nome_usuario", nomeUsuarioEt.text.toString())
        edit.putString("endereco", enderecoEt.text.toString())
        edit.putString("genero", generoEt.text.toString())
        edit.putString("telefone", telefoneEt.text.toString())
        edit.apply()

        // Atualiza Firestore
        db.collection("funcionarios").document(docId)
            .update(mapa)
            .addOnSuccessListener {
                Log.d("TelaInfoFuncionario", "Dados atualizados com sucesso")
            }
            .addOnFailureListener { e ->
                Log.e("TelaInfoFuncionario", "Erro ao salvar dados", e)
            }
    }

    override fun onStart() {
        super.onStart()
        Log.d("TelaInfoFuncionario", "onStart chamado")
    }

    override fun onPause() {
        super.onPause()
        Log.d("TelaInfoFuncionario", "onPause chamado")
    }

    override fun onStop() {
        super.onStop()
        salvarDados()
        Log.d("TelaInfoFuncionario", "onStop chamado")
    }

    override fun onRestart() {
        super.onRestart()
        Log.d("TelaInfoFuncionario", "onRestart chamado")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("TelaInfoFuncionario", "onDestroy chamado")
    }
}



























/*
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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
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

    private lateinit var funcionarioDocId: String

    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_tela_informacao_pessoal_funcionario)

        btnSetaVoltar = findViewById(R.id.SetaVoltarTelaGerenciamentoAluno)
        btnNavegacao = findViewById(R.id.bottom_navigation)

        db = Firebase.firestore

        */
/*campoNomeUsuario = findViewById(R.id.campo_nome_usuario)
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


        funcionarioDocId = prefs.getString("usuarioDocId", null).toString()

        campoNomeUsuario.setText(nomeUsuario)
        campoEnderecoUsuario.setText(enderecoUsuario)
        generoUsuario.setText(generoUsuario1)
        numeroContato.setText(numeroContato1)
        primeiro_nome_usuario.text = nome
        sobrenome_usuario.text = sobrenomeUsuario
        campo_idade_usuario.text = idadeUsuario*//*





        configurarEventos()

    }

    override fun onBackPressed() {
        // garante que salve antes de sair
        salvarDados()
        super.onBackPressed()
    }

    fun configurarEventos(){

        // Definindo o clique do botão de voltar
        btnSetaVoltar.setOnClickListener {
            salvarDados()
            finish() // Isso chama o comportamento de voltar para a tela anterior
        }


        btnNavegacao.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> {
                    // O que acontece quando o item "Início" é clicado
                    val intent = Intent(this, TelaFuncionario::class.java)
                    salvarDados()
                    startActivity(intent)
                    true
                }

                R.id.nav_chat -> {
                    // Abre a tela de chat
                    val intent = Intent(this, TelaChat::class.java)
                    salvarDados()
                    startActivity(intent)
                    true
                }

                R.id.nav_config -> {
                    // Abre a tela de configurações
                    val intent = Intent(this, TelaConfiguracao_Funcionario::class.java)
                    salvarDados()
                    startActivity(intent)
                    true
                }

                else -> false
            }
        }
    }


    private fun salvarDados() {
        val docId = funcionarioDocId
        val mapa = mapOf(
            "nome_usuario" to campoNomeUsuario.text.toString(),
            "endereco"     to campoEnderecoUsuario.text.toString(),
            "genero"       to generoUsuario.text.toString(),
            "telefone"     to numeroContato.text.toString()
        )

        var prefs = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
        var edit = prefs.edit()

        edit.putString("nome_usuario", campoNomeUsuario.text.toString())
        edit.putString("endereco", campoEnderecoUsuario.text.toString())
        edit.putString("genero", generoUsuario.text.toString())
        edit.putString("telefone", numeroContato.text.toString())
        edit.apply()


        db.collection("alunos").document(docId)
            .update(mapa)
            .addOnSuccessListener {
                Log.d("TelaInfoFuncionario", "Dados atualizados com sucesso")
//                Toast.makeText(this, "Alterações salvas", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("TelaInfoFuncionario", "Erro ao salvar dados", e)
//                Toast.makeText(this, "Falha ao salvar alterações.", Toast.LENGTH_SHORT).show()
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
}*/
