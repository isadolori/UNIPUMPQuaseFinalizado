package com.example.unipump

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.unipump.adapters.ExerciciosAdapter
import com.example.unipump.models.Exercicio
import com.example.unipump.models.Serie
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore

class TelaFichaTreino : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val alunoId = "UjJe5GoXPQeqhmwiAiib"
    private val nomeSubColecao = "treino"

    // Guarda o ID do documento de treino para o update
    private var docIdTreino: String? = null

    // Lista mutável de exercícios (cada Exercicio contém sua lista de Series)
    private val listaExercicios = mutableListOf<Exercicio>()
    private lateinit var exercAdapter: ExerciciosAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tela_ficha_treino)

        // 1. Botão Voltar
        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            finish()
        }

        // 2. Configura RecyclerView + Adapter (adapter deve gerenciar edição de cada Serie)
        val rv = findViewById<RecyclerView>(R.id.rvExercicios).apply {
            layoutManager = LinearLayoutManager(this@TelaFichaTreino)
        }
        exercAdapter = ExerciciosAdapter(listaExercicios)
        rv.adapter = exercAdapter

        // 3. Bottom Navigation
        findViewById<BottomNavigationView>(R.id.bottom_navigation)
            .setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_inicio   -> true
                    R.id.nav_treinos  -> { startActivity(Intent(this, TelaTreinoAluno::class.java)); true }
                    R.id.nav_config   -> { startActivity(Intent(this, TelaConfig::class.java)); true }
                    R.id.nav_chat     -> { startActivity(Intent(this, TelaChat::class.java)); true }
                    else              -> false
                }
            }

        // 4. Botão Iniciar: salva as séries editadas e então abre TelaExercicioFinalizado
        findViewById<Button>(R.id.buttonStart).setOnClickListener {
            salvarTreino { sucesso ->
                if (sucesso) {
                    startActivity(Intent(this, TelaExercicioFinalizado::class.java))
                } else {
                    Toast.makeText(this, "Falha ao salvar treino", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 5. Carrega do Firestore
        carregarExerciciosAluno { lista, docId ->
            docIdTreino = docId
            listaExercicios.clear()
            listaExercicios.addAll(lista)
            exercAdapter.notifyDataSetChanged()
        }
    }

    private fun carregarExerciciosAluno(onComplete: (List<Exercicio>, String?) -> Unit) {
        db.collection("alunos")
            .document(alunoId)
            .collection(nomeSubColecao)
            .limit(1)
            .get()
            .addOnSuccessListener { snaps ->
                val doc = snaps.documents.firstOrNull()
                if (doc == null) {
                    Toast.makeText(this, "Nenhum treino encontrado", Toast.LENGTH_SHORT).show()
                    onComplete(emptyList(), null)
                    return@addOnSuccessListener
                }

                val raw = doc.get("exercicios") as? List<Map<String,Any>> ?: emptyList()
                val lista = raw.map { m ->
                    val seriesRaw = m["series"] as? List<Map<String,Any>> ?: emptyList()
                    val series = seriesRaw.map { s ->
                        Serie(
                            ordem    = s["ordem"]?.toString().orEmpty(),
                            reps     = s["reps"]?.toString().orEmpty(),
                            peso     = s["peso"]?.toString().orEmpty(),
                            descanso = s["descanso"]?.toString().orEmpty()
                        )
                    }
                    Exercicio(
                        frame    = m["frame"]?.toString().orEmpty(),
                        execucao = m["execucao"]?.toString().orEmpty(),
                        nome     = m["nome"]?.toString().orEmpty(),
                        series   = series
                    )
                }

                onComplete(lista, doc.id)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao carregar treino", Toast.LENGTH_SHORT).show()
                onComplete(emptyList(), null)
            }
    }

    private fun salvarTreino(onResult: (Boolean) -> Unit) {
        val docId = docIdTreino
        if (docId.isNullOrEmpty()) {
            onResult(false)
            return
        }

        // Reconstrói o payload usando os valores atualizados em cada Serie
        val payload = listaExercicios.map { ex ->
            mapOf(
                "frame"    to ex.frame,
                "execucao" to ex.execucao,
                "nome"     to ex.nome,
                "series"   to ex.series.map { s ->
                    mapOf(
                        "ordem"    to s.ordem,
                        "reps"     to s.reps,
                        "peso"     to s.peso,
                        "descanso" to s.descanso
                    )
                }
            )
        }

        db.collection("alunos")
            .document(alunoId)
            .collection(nomeSubColecao)
            .document(docId)
            .update("exercicios", payload)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }
}
