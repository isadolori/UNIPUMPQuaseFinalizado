package com.example.unipump

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore

class TelaInformacoesPessoaisAluno : AppCompatActivity() {

    // Views
    private lateinit var nomeUsuarioEt:  EditText
    private lateinit var enderecoEt:     EditText
    private lateinit var generoEt:       EditText
    private lateinit var telefoneEt:     EditText
    private lateinit var primeiroNomeTv: TextView
    private lateinit var sobrenomeTv:    TextView
    private lateinit var idadeTv:        TextView
//    private lateinit var salvarBtn:      Button
    private lateinit var btnVoltar:      ImageView
    private lateinit var bottomNav:      BottomNavigationView

    // Firestore
    private val db by lazy { FirebaseFirestore.getInstance() }

    // SharedPreferences onde guardamos o alunoDocId no login
    private val prefs by lazy {
        getSharedPreferences("alunoPrefs", MODE_PRIVATE)
    }

    // recupera o ID de documento Firestore do aluno
    private val alunoDocId: String?
        get() = prefs.getString("alunoDocId", null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tela_informacoes_pessoais_aluno)

        // 1) Bind das views
        nomeUsuarioEt  = findViewById(R.id.nome_usuario)
        enderecoEt     = findViewById(R.id.endereco_usuario)
        generoEt       = findViewById(R.id.genero_usuario)
        telefoneEt     = findViewById(R.id.numero_contato_usuario)
        primeiroNomeTv = findViewById(R.id.primeiro_nome_usuario)
        sobrenomeTv    = findViewById(R.id.sobrenome_usuario)
        idadeTv        = findViewById(R.id.idade_usuario)
//        salvarBtn      = findViewById(R.id.btn_salvar)
        btnVoltar      = findViewById(R.id.btn_back)
        bottomNav      = findViewById(R.id.bottom_navigation)

        // 2) Configura botões de salvar e voltar
//        salvarBtn.setOnClickListener {
//            salvarDados()
//        }

        btnVoltar.setOnClickListener {
            salvarDados()
            finish()
        }

        // 3) Bottom navigation
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio   -> {
                    startActivity(Intent(this, TelaPrincipalAluno::class.java))
                    salvarDados()

                }
                R.id.nav_treinos  -> {
                    startActivity(Intent(this, TelaTreinoAluno::class.java))
                    salvarDados()
                }
                R.id.nav_chat     -> {
                    startActivity(Intent(this, TelaChat::class.java))
                    salvarDados()
                }
            }
            true
        }

        // 4) Puxa o alunoDocId e busca dados
        alunoDocId?.let { docId ->
            buscarDadosAluno(docId)
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
        Log.d("Tela Informações", "onResume chamado")
        // recarrega sempre que a Activity volta ao foco
        alunoDocId?.let { buscarDadosAluno(it) }
    }

    override fun onBackPressed() {
        // garante que salve antes de sair
        salvarDados()
        super.onBackPressed()
    }

    private fun buscarDadosAluno(docId: String) {
        Log.d("TelaInfoAluno", "Buscando dados em alunos/$docId")
        db.collection("alunos").document(docId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    nomeUsuarioEt.setText(   doc.getString("nome_usuario") )
                    enderecoEt.setText(      doc.getString("endereco")     )
                    generoEt.setText(        doc.getString("genero")       )
                    telefoneEt.setText(      doc.getString("telefone")     )
                    primeiroNomeTv.text =    doc.getString("nome") ?: ""
                    sobrenomeTv.text =       doc.getString("sobrenome") ?: ""
                    idadeTv.text =           doc.getString("idade") ?: ""
                } else {
                    Log.w("TelaInfoAluno", "Documento não existe.")
                }
            }
            .addOnFailureListener { e ->
                Log.e("TelaInfoAluno", "Erro ao buscar dados", e)
                Toast.makeText(this, "Falha ao carregar seus dados.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun salvarDados() {
        val docId = alunoDocId ?: return
        val mapa = mapOf(
            "nome_usuario" to nomeUsuarioEt.text.toString(),
            "endereco"     to enderecoEt.text.toString(),
            "genero"       to generoEt.text.toString(),
            "telefone"     to telefoneEt.text.toString()
        )
        db.collection("alunos").document(docId)
            .update(mapa)
            .addOnSuccessListener {
                Log.d("TelaInfoAluno", "Dados atualizados com sucesso")
//                Toast.makeText(this, "Alterações salvas", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("TelaInfoAluno", "Erro ao salvar dados", e)
//                Toast.makeText(this, "Falha ao salvar alterações.", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onStart() {
        super.onStart()
        Log.d("Tela Informações","onStart chamado")

    }


    override fun onPause() {
        super.onPause()
        Log.d("Tela Informações", "onPause chamado")

    }

    override fun onStop() {
        super.onStop()
        salvarDados()
        Log.d("Tela Informações", "onStop chamado")
    }

    override fun onRestart() {
        super.onRestart()
        Log.d("Tela Informações", "onRestart chamado")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("Tela Informações", "onDestroy chamado")
    }


}
