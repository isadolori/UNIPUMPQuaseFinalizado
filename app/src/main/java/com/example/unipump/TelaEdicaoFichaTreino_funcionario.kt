package com.example.unipump

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.ScrollView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.unipump.adapters.FichaTreinoFunAdapter
import com.example.unipump.models.exercicioFun
import com.example.unipump.models.fichaTreinoFun
import com.example.unipump.models.serieFun
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Timer
import java.util.TimerTask

class TelaEdicaoFichaTreino_funcionario : AppCompatActivity() {

    private lateinit var btnNavegacao: BottomNavigationView
    private lateinit var btnSetaVoltar: ImageButton
    private lateinit var btnAdicionarExercicio: Button
    private lateinit var recyclerViewFichas: RecyclerView
    private lateinit var scrollView: ScrollView

    // Firestore
    private lateinit var db: FirebaseFirestore
    private var alunoDocId: String = ""
    private var documentId: String = "" // NOVO: ID único do documento no Firestore
    private var fichaLetra: String = ""
    private var fichaNome: String = ""
    private var fichaDescricao: String = ""

    // DEPRECADO: fichaId será substituído por documentId
    @Deprecated("Use documentId instead")
    private var fichaId: String = ""

    // Adapter - Uma única ficha será exibida, mas mantemos a estrutura de lista
    private lateinit var fichaTreinoAdapter: FichaTreinoFunAdapter
    private val fichasList = mutableListOf<fichaTreinoFun>()

    // Flag para controlar se há alterações pendentes
    private var hasUnsavedChanges = false
    private var saveDebounceTimer: Timer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            enableEdgeToEdge()
            setContentView(R.layout.activity_tela_edicao_ficha_treino_funcionario)

            // Inicializar Firestore
            db = FirebaseFirestore.getInstance()

            // Recuperar dados da ficha específica
            if (!recuperarDadosIntent()) {
                return // Se não conseguir recuperar dados, sai da activity
            }

            // Inicializar views
            initViews()

            // Configurar título da ficha
            configurarTituloFicha()

            // Configurar RecyclerView
            setupRecyclerView()

            // Debug da estrutura do Firestore
            debugFirestoreStructure()

            // Configurar eventos
            configurarEventos()

            // Carregar APENAS a ficha específica do Firestore
            carregarFichaEspecificaDoFirestore()

        } catch (e: Exception) {
            Log.e("EDICAO_FICHA_ERROR", "Erro no onCreate", e)
            Toast.makeText(this, "Erro ao inicializar tela: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    // MÉTODO CORRIGIDO: Agora recupera o documentId
    private fun recuperarDadosIntent(): Boolean {
        return try {
            // CRÍTICO: Recuperar o documentId passado da tela anterior
            documentId = intent.getStringExtra("documentId") ?: ""
            alunoDocId = intent.getStringExtra("alunoDocId") ?: ""
            fichaLetra = intent.getStringExtra("ficha_letra") ?: ""
            fichaNome = intent.getStringExtra("ficha_nome") ?: ""
            fichaDescricao = intent.getStringExtra("ficha_descricao") ?: ""

            // Para compatibilidade com código antigo
            fichaId = documentId

            Log.d("EDICAO_FICHA", "=== DADOS RECUPERADOS ===")
            Log.d("EDICAO_FICHA", "Document ID: $documentId")
            Log.d("EDICAO_FICHA", "Aluno ID: $alunoDocId")
            Log.d("EDICAO_FICHA", "Ficha Letra: $fichaLetra")
            Log.d("EDICAO_FICHA", "Ficha Nome: $fichaNome")

            // Se não veio por intent, tentar recuperar do SharedPreferences
            if (alunoDocId.isEmpty()) {
                val prefs = getSharedPreferences("alunoPrefs", MODE_PRIVATE)
                alunoDocId = prefs.getString("alunoDocId", "") ?: ""
                Log.d("EDICAO_FICHA", "Aluno ID recuperado do SharedPreferences: $alunoDocId")
            }

            // Verificações de segurança
            if (documentId.isEmpty()) {
                Toast.makeText(this, "ERRO: ID do documento não encontrado!", Toast.LENGTH_LONG).show()
                Log.e("EDICAO_FICHA", "Document ID está vazio - não será possível salvar alterações!")
                finish()
                false
            } else if (alunoDocId.isEmpty()) {
                Toast.makeText(this, "ERRO: ID do aluno não encontrado!", Toast.LENGTH_LONG).show()
                Log.e("EDICAO_FICHA", "Aluno ID está vazio!")
                finish()
                false
            } else if (fichaLetra.isEmpty()) {
                Toast.makeText(this, "ERRO: Letra da ficha não encontrada!", Toast.LENGTH_SHORT).show()
                Log.e("EDICAO_FICHA", "Ficha letra está vazia!")
                finish()
                false
            } else {
                Log.d("EDICAO_FICHA", "✅ Todos os dados necessários foram recuperados com sucesso")
                true
            }
        } catch (e: Exception) {
            Log.e("EDICAO_FICHA_ERROR", "Erro ao recuperar dados do intent", e)
            Toast.makeText(this, "Erro ao recuperar dados", Toast.LENGTH_SHORT).show()
            finish()
            false
        }
    }

    private fun initViews() {
        try {
            btnNavegacao = findViewById(R.id.bottom_navigation)
            btnSetaVoltar = findViewById(R.id.SetaVoltarTelaEdicaoFicha)
            btnAdicionarExercicio = findViewById(R.id.btnAdicionar)
            recyclerViewFichas = findViewById(R.id.recyclerViewFichas)
            scrollView = findViewById(R.id.scrollView)

            Log.d("EDICAO_FICHA", "Views inicializadas com sucesso")
        } catch (e: Exception) {
            Log.e("EDICAO_FICHA_ERROR", "Erro ao inicializar views", e)
            throw e
        }
    }

    private fun configurarTituloFicha() {
        Log.d("EDICAO_FICHA", "Título configurado para: Ficha $fichaLetra - $fichaNome")
    }

    private fun setupRecyclerView() {
        // Configurar adapter com callbacks
        fichaTreinoAdapter = FichaTreinoFunAdapter(
            fichas = fichasList,
            onExcluirFicha = { fichaIdCallback: String, position: Int ->
                // Callback para excluir ficha completa
                excluirFichaCompleta(fichaIdCallback, position)
            },
            onFichaAlterada = { fichaAlterada: fichaTreinoFun, position: Int ->
                // CALLBACK PRINCIPAL: Quando qualquer coisa na ficha for alterada
                Log.d("FICHA_ALTERADA", "Ficha alterada: ${fichaAlterada.letra} - ${fichaAlterada.nome}")
                Log.d("FICHA_ALTERADA", "Total de exercícios: ${fichaAlterada.exercicios.size}")

                // Listar exercícios e suas séries para debug
                fichaAlterada.exercicios.forEachIndexed { exIndex, exercicio ->
                    Log.d("FICHA_ALTERADA", "  Exercício $exIndex: ${exercicio.nome} (${exercicio.series.size} séries)")
                    exercicio.series.forEachIndexed { serieIndex, serie ->
                        Log.d("FICHA_ALTERADA", "    Série ${serie.numero}: ${serie.repeticoes} reps, ${serie.peso}, ${serie.tempo}s")
                    }
                }

                // Chamar o método que já existe para alterações
                onFichaAlterada(fichaAlterada, position)
            }
        )

        recyclerViewFichas.apply {
            layoutManager = LinearLayoutManager(this@TelaEdicaoFichaTreino_funcionario)
            adapter = fichaTreinoAdapter

            // Configurações otimizadas
            isNestedScrollingEnabled = false
            setHasFixedSize(false) // Permitir altura variável
            itemAnimator = null // Remover animações para melhor performance
        }

        // Debug do RecyclerView
        recyclerViewFichas.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            Log.d("RECYCLERVIEW_DEBUG", "=== LAYOUT CHANGE ===")
            Log.d("RECYCLERVIEW_DEBUG", "Altura do RecyclerView: ${recyclerViewFichas.height}")
            Log.d("RECYCLERVIEW_DEBUG", "Itens visíveis: ${recyclerViewFichas.childCount}")
            Log.d("RECYCLERVIEW_DEBUG", "Adapter count: ${fichaTreinoAdapter.itemCount}")

            // Verificar altura de cada item
            for (i in 0 until recyclerViewFichas.childCount) {
                val child = recyclerViewFichas.getChildAt(i)
                Log.d("RECYCLERVIEW_DEBUG", "Item $i altura: ${child.height}px")
            }
            Log.d("RECYCLERVIEW_DEBUG", "========================")
        }
    }

    // MÉTODO CORRIGIDO: Agora usa o documentId diretamente
    private fun debugFirestoreStructure() {
        Log.d("DEBUG_FIRESTORE", "=== INICIANDO DEBUG DA ESTRUTURA ESPECÍFICA ===")
        Log.d("DEBUG_FIRESTORE", "Document ID: $documentId")
        Log.d("DEBUG_FIRESTORE", "Aluno ID: $alunoDocId")

        db.collection("alunos")
            .document(alunoDocId)
            .collection("treino")
            .document(documentId) // USAR DOCUMENT ID ESPECÍFICO
            .get()
            .addOnSuccessListener { document ->
                Log.d("DEBUG_FIRESTORE", "Documento encontrado: ${document.exists()}")

                if (document.exists()) {
                    Log.d("DEBUG_FIRESTORE", "=== Dados do Documento ===")
                    Log.d("DEBUG_FIRESTORE", "ID: ${document.id}")
                    Log.d("DEBUG_FIRESTORE", "Letra: ${document.getString("letra")}")
                    Log.d("DEBUG_FIRESTORE", "Nome: ${document.getString("nome")}")

                    // Debug específico dos exercícios
                    val exercicios = document.get("exercicios")
                    Log.d("DEBUG_FIRESTORE", "Exercícios (objeto completo): $exercicios")
                    Log.d("DEBUG_FIRESTORE", "Tipo dos exercícios: ${exercicios?.javaClass?.simpleName}")

                    if (exercicios is List<*>) {
                        Log.d("DEBUG_FIRESTORE", "Exercícios é uma lista com ${exercicios.size} itens")
                        exercicios.forEachIndexed { exIndex, exercicio ->
                            Log.d("DEBUG_FIRESTORE", "  Exercício $exIndex: $exercicio")
                            if (exercicio is Map<*, *>) {
                                val nome = exercicio["nome"]
                                val series = exercicio["series"]
                                Log.d("DEBUG_FIRESTORE", "    Nome: $nome")
                                Log.d("DEBUG_FIRESTORE", "    Séries: $series")
                                if (series is List<*>) {
                                    Log.d("DEBUG_FIRESTORE", "    Número de séries: ${series.size}")
                                }
                            }
                        }
                    } else {
                        Log.w("DEBUG_FIRESTORE", "Exercícios NÃO é uma lista! Tipo: ${exercicios?.javaClass}")
                    }
                } else {
                    Log.w("DEBUG_FIRESTORE", "Documento não existe!")
                }

                Log.d("DEBUG_FIRESTORE", "=== FIM DO DEBUG ===")
            }
            .addOnFailureListener { e ->
                Log.e("DEBUG_FIRESTORE", "Erro no debug", e)
                e.printStackTrace()
            }
    }

    // Método chamado quando uma ficha é alterada
    private fun onFichaAlterada(fichaAlterada: fichaTreinoFun, position: Int) {
        Log.d("FICHA_ALTERADA", "Ficha alterada: ${fichaAlterada.letra} - ${fichaAlterada.nome}")

        // Marcar que há alterações pendentes
        hasUnsavedChanges = true

        // Cancelar timer anterior
        saveDebounceTimer?.cancel()

        // Criar novo timer com delay de 1 segundo (debounce)
        saveDebounceTimer = Timer()
        saveDebounceTimer?.schedule(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    Log.d("DEBOUNCE_SAVE", "Executando salvamento com debounce")
                    salvarFichaAlteradaNoFirestore(fichaAlterada)
                }
            }
        }, 1000) // Aguarda 1 segundo após a última alteração
    }

    // MÉTODO CORRIGIDO: Agora usa documentId para salvar
    private fun salvarFichaAlteradaNoFirestore(ficha: fichaTreinoFun) {
        Log.d("FIRESTORE_SAVE_IMMEDIATE", "=== SALVANDO ALTERAÇÕES ===")
        Log.d("FIRESTORE_SAVE_IMMEDIATE", "Document ID: $documentId")
        Log.d("FIRESTORE_SAVE_IMMEDIATE", "Ficha: ${ficha.letra} - ${ficha.nome}")

        if (documentId.isEmpty() || alunoDocId.isEmpty()) {
            Log.e("FIRESTORE_SAVE_IMMEDIATE", "❌ IDs inválidos")
            return
        }

        // CRÍTICO: Usar valores atuais do objeto ficha (que já foi atualizado pelo adapter)
        val letraAtualizada = ficha.letra
        val nomeAtualizado = ficha.nome

        Log.d("FIRESTORE_SAVE_IMMEDIATE", "Valores a salvar - Letra: '$letraAtualizada', Nome: '$nomeAtualizado'")

        // Converter exercícios para o formato do Firestore
        val exerciciosArray = ficha.exercicios.map { exercicio ->
            mapOf(
                "nome" to exercicio.nome,
                "execucao" to "Execução normal",
                "frame" to "URL...",
                "series" to exercicio.series.map { serie ->
                    mapOf(
                        "ordem" to serie.numero.toString(),
                        "reps" to serie.repeticoes.toString(),
                        "peso" to serie.peso,
                        "descanso" to serie.tempo
                    )
                }
            )
        }

        val fichaData = hashMapOf(
            "letra" to letraAtualizada,
            "nome" to nomeAtualizado,
            "descricao" to fichaDescricao,
            "quantidadeExercicios" to ficha.exercicios.size,
            "exercicios" to exerciciosArray,
            "dataModificacao" to com.google.firebase.Timestamp.now()
        )

        Log.d("FIRESTORE_SAVE_IMMEDIATE", "Caminho: alunos/$alunoDocId/treino/$documentId")

        db.collection("alunos")
            .document(alunoDocId)
            .collection("treino")
            .document(documentId)
            .update(fichaData as Map<String, Any>)
            .addOnSuccessListener {
                Log.d("FIRESTORE_SAVE_IMMEDIATE", "✅ Ficha '$letraAtualizada' salva com sucesso!")
                hasUnsavedChanges = false

                // Atualizar variáveis locais para sincronia
                fichaLetra = letraAtualizada
                fichaNome = nomeAtualizado

                Log.d("FIRESTORE_SAVE_IMMEDIATE", "Variáveis locais atualizadas")
            }
            .addOnFailureListener { e ->
                Log.e("FIRESTORE_SAVE_IMMEDIATE", "❌ Erro ao salvar ficha", e)
                hasUnsavedChanges = true

                runOnUiThread {
                    Toast.makeText(this@TelaEdicaoFichaTreino_funcionario,
                        "Erro ao salvar: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }

                // Retry após 5 segundos
                Handler(Looper.getMainLooper()).postDelayed({
                    if (hasUnsavedChanges) {
                        Log.d("RETRY_SAVE", "Tentando salvar novamente após erro")
                        salvarFichaAlteradaNoFirestore(ficha)
                    }
                }, 5000)
            }
    }

    private fun excluirFichaCompleta(fichaIdCallback: String, position: Int) {
        Log.d("EXCLUIR_FICHA", "Iniciando exclusão da ficha ID: $fichaIdCallback, posição: $position")

        // 1. Excluir do Firestore primeiro
        excluirFichaDoFirestore(fichaIdCallback) { sucesso ->
            runOnUiThread {
                if (sucesso) {
                    // 2. Se excluiu do Firestore com sucesso, remover da tela
                    fichaTreinoAdapter.removeFicha(position)

                    Toast.makeText(
                        this@TelaEdicaoFichaTreino_funcionario,
                        "Ficha excluída com sucesso",
                        Toast.LENGTH_SHORT
                    ).show()

                    Log.d("EXCLUIR_FICHA", "Ficha excluída com sucesso")

                    // 3. Se era a única ficha, voltar para tela anterior
                    if (fichasList.isEmpty()) {
                        Toast.makeText(
                            this@TelaEdicaoFichaTreino_funcionario,
                            "Não há mais fichas para editar",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Voltar para tela anterior
                        finish()
                    }

                } else {
                    Toast.makeText(
                        this@TelaEdicaoFichaTreino_funcionario,
                        "Erro ao excluir ficha",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun excluirFichaDoFirestore(fichaIdToDelete: String, callback: (Boolean) -> Unit) {
        Log.d("FIRESTORE_DELETE", "Excluindo ficha do Firestore - ID: $fichaIdToDelete")

        db.collection("alunos")
            .document(alunoDocId)
            .collection("treino")
            .document(fichaIdToDelete)
            .delete()
            .addOnSuccessListener {
                Log.d("FIRESTORE_DELETE", "Ficha excluída do Firestore com sucesso")
                callback(true)
            }
            .addOnFailureListener { exception ->
                Log.e("FIRESTORE_DELETE", "Erro ao excluir ficha do Firestore", exception)
                callback(false)
            }
    }

    // MÉTODO CORRIGIDO: Agora carrega usando documentId diretamente
    private fun carregarFichaEspecificaDoFirestore() {
        try {
            fichasList.clear()

            Log.d("FIRESTORE_EDICAO", "=== CARREGANDO FICHA ESPECÍFICA ===")
            Log.d("FIRESTORE_EDICAO", "Document ID: $documentId")
            Log.d("FIRESTORE_EDICAO", "Aluno ID: $alunoDocId")

            // BUSCAR DIRETAMENTE PELO DOCUMENT ID (MAIS EFICIENTE)
            db.collection("alunos")
                .document(alunoDocId)
                .collection("treino")
                .document(documentId) // USAR DOCUMENT ID ESPECÍFICO
                .get()
                .addOnSuccessListener { document ->
                    try {
                        Log.d("FIRESTORE_EDICAO", "Documento encontrado: ${document.exists()}")

                        if (!document.exists()) {
                            Log.w("FIRESTORE_EDICAO", "Documento não encontrado: $documentId")
                            runOnUiThread {
                                Toast.makeText(
                                    this@TelaEdicaoFichaTreino_funcionario,
                                    "Ficha não encontrada", Toast.LENGTH_SHORT
                                ).show()
                                finish() // Voltar se não encontrar
                            }
                            return@addOnSuccessListener
                        }

                        // Processar o documento
                        val letra = document.getString("letra") ?: ""
                        val nome = document.getString("nome") ?: ""

                        Log.d("FIRESTORE_EDICAO", "Processando ficha: $documentId - Letra: $letra - Nome: $nome")

                        // Debug dos exercícios
                        val exerciciosRaw = document.get("exercicios")
                        Log.d("FIRESTORE_EDICAO", "Exercícios RAW do Firestore: $exerciciosRaw")
                        Log.d("FIRESTORE_EDICAO", "Tipo dos exercícios: ${exerciciosRaw?.javaClass?.simpleName}")

                        val exerciciosArray = document.get("exercicios") as? List<Map<String, Any>> ?: emptyList()
                        Log.d("FIRESTORE_EDICAO", "Array de exercícios convertido - Tamanho: ${exerciciosArray.size}")

                        val exerciciosList = mutableListOf<exercicioFun>()

                        // Processamento dos exercícios
                        exerciciosArray.forEachIndexed { exercicioIndex, exercicioMap ->
                            try {
                                Log.d("FIRESTORE_EDICAO", "=== Processando exercício $exercicioIndex ===")
                                Log.d("FIRESTORE_EDICAO", "Dados do exercício: $exercicioMap")

                                val nomeExercicio = exercicioMap["nome"]?.toString() ?: ""
                                Log.d("FIRESTORE_EDICAO", "Nome do exercício: $nomeExercicio")

                                // Debug das séries
                                val seriesRaw = exercicioMap["series"]
                                Log.d("FIRESTORE_EDICAO", "Séries RAW: $seriesRaw")
                                Log.d("FIRESTORE_EDICAO", "Tipo das séries: ${seriesRaw?.javaClass?.simpleName}")

                                val seriesArray = exercicioMap["series"] as? List<Map<String, Any>> ?: emptyList()
                                Log.d("FIRESTORE_EDICAO", "Array de séries convertido - Tamanho: ${seriesArray.size}")

                                val seriesList = seriesArray.mapIndexed { serieIndex, serieMap ->
                                    Log.d("FIRESTORE_EDICAO", "   Série $serieIndex: $serieMap")

                                    serieFun(
                                        id = "${documentId}_ex${exercicioIndex}_serie${serieIndex}",
                                        numero = serieMap["ordem"]?.toString()?.toIntOrNull() ?: (serieIndex + 1),
                                        repeticoes = serieMap["reps"]?.toString()?.toIntOrNull() ?: 0,
                                        peso = serieMap["peso"]?.toString() ?: "",
                                        tempo = serieMap["descanso"]?.toString() ?: ""
                                    )
                                }.toMutableList()

                                val exercicio = exercicioFun(
                                    id = "${documentId}_exercicio_$exercicioIndex",
                                    nome = nomeExercicio,
                                    series = seriesList
                                )

                                exerciciosList.add(exercicio)
                                Log.d("FIRESTORE_EDICAO", "Exercício adicionado: $nomeExercicio com ${seriesList.size} séries")
                                Log.d("FIRESTORE_EDICAO", "Total de exercícios na lista agora: ${exerciciosList.size}")

                            } catch (e: Exception) {
                                Log.e("FIRESTORE_EDICAO", "Erro ao processar exercício $exercicioIndex", e)
                                e.printStackTrace()
                            }
                        }

                        // Log final antes de criar a ficha
                        Log.d("FIRESTORE_EDICAO", "RESUMO FINAL:")
                        Log.d("FIRESTORE_EDICAO", "Total de exercícios processados: ${exerciciosList.size}")
                        exerciciosList.forEachIndexed { index, exercicio ->
                            Log.d("FIRESTORE_EDICAO", "Exercício $index: ${exercicio.nome} (${exercicio.series.size} séries)")
                        }

                        val ficha = fichaTreinoFun(
                            id = documentId, // USAR DOCUMENT ID
                            letra = letra,
                            nome = nome,
                            exercicios = exerciciosList
                        )

                        fichasList.add(ficha)
                        Log.d("FIRESTORE_EDICAO", "Ficha carregada: $letra - $nome com ${exerciciosList.size} exercícios")

                        // Atualizar UI
                        runOnUiThread {
                            try {
                                if (::fichaTreinoAdapter.isInitialized) {
                                    Log.d("FIRESTORE_EDICAO", "Notificando adapter com ${fichasList.size} fichas")
                                    if (fichasList.isNotEmpty()) {
                                        Log.d("FIRESTORE_EDICAO", "Primeira ficha tem ${fichasList.first().exercicios.size} exercícios")
                                    }

                                    fichaTreinoAdapter.notifyDataSetChanged()

                                    // Verificação adicional após notificar
                                    recyclerViewFichas.post {
                                        Log.d("FIRESTORE_EDICAO", "Após notifyDataSetChanged:")
                                        Log.d("FIRESTORE_EDICAO", "Adapter item count: ${fichaTreinoAdapter.itemCount}")
                                        Log.d("FIRESTORE_EDICAO", "RecyclerView child count: ${recyclerViewFichas.childCount}")

                                        // Forçar atualização dos contadores
                                        fichaTreinoAdapter.atualizarContadores()
                                    }
                                }

                                Toast.makeText(
                                    this@TelaEdicaoFichaTreino_funcionario,
                                    "Ficha $letra carregada com ${fichasList.firstOrNull()?.exercicios?.size ?: 0} exercícios",
                                    Toast.LENGTH_SHORT
                                ).show()

                            } catch (e: Exception) {
                                Log.e("FIRESTORE_EDICAO", "Erro ao atualizar UI", e)
                                e.printStackTrace()
                            }
                        }

                    } catch (e: Exception) {
                        Log.e("FIRESTORE_EDICAO", "Erro geral ao processar ficha", e)
                        e.printStackTrace()
                        runOnUiThread {
                            Toast.makeText(
                                this@TelaEdicaoFichaTreino_funcionario,
                                "Erro ao processar ficha: ${e.message}", Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("FIRESTORE_EDICAO", "Erro ao carregar ficha específica", exception)
                    exception.printStackTrace()
                    runOnUiThread {
                        Toast.makeText(
                            this@TelaEdicaoFichaTreino_funcionario,
                            "Erro ao carregar ficha: ${exception.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

        } catch (e: Exception) {
            Log.e("FIRESTORE_EDICAO", "Erro ao iniciar carregamento da ficha", e)
            e.printStackTrace()
            Toast.makeText(this, "Erro ao carregar ficha: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun configurarEventos() {
        try {
            btnNavegacao.setOnNavigationItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_inicio -> {
                        val intent = Intent(this, TelaFuncionario::class.java)
                        startActivity(intent)
                        true
                    }
                    R.id.nav_chat -> {
                        val intent = Intent(this, TelaChat::class.java)
                        startActivity(intent)
                        true
                    }
                    R.id.nav_config -> {
                        val intent = Intent(this, TelaConfiguracao_Funcionario::class.java)
                        startActivity(intent)
                        true
                    }
                    else -> false
                }
            }

            btnSetaVoltar.setOnClickListener {
                Log.d("LIFECYCLE", "Botão voltar pressionado - Salvando alterações")
                salvarTodasAlteracoesPendentes()
                finish() // Volta para a tela anterior
            }

            // Configurar botão adicionar exercício
            try {
                btnAdicionarExercicio.setOnClickListener {
                    Log.d("ADD_EXERCICIO_BTN", "Botão adicionar exercício clicado")
                    adicionarNovoExercicio()
                }
            } catch (e: Exception) {
                Log.w("EDICAO_FICHA", "Botão adicionar exercício não encontrado no layout: ${e.message}")
            }

            Log.d("EDICAO_FICHA", "Eventos configurados com sucesso")

        } catch (e: Exception) {
            Log.e("EDICAO_FICHA_ERROR", "Erro ao configurar eventos", e)
            throw e
        }
    }

    // Método para adicionar novo exercício à ficha atual
    private fun adicionarNovoExercicio() {
        if (fichasList.isEmpty()) {
            Toast.makeText(this, "Nenhuma ficha carregada", Toast.LENGTH_SHORT).show()
            return
        }

        // Mostrar diálogo para inserir nome do exercício
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        val input = android.widget.EditText(this).apply {
            hint = "Nome do exercício"
            setPadding(32, 32, 32, 32)
        }

        builder.setTitle("Adicionar Exercício")
            .setView(input)
            .setPositiveButton("Adicionar") { _, _ ->
                val nomeExercicio = input.text.toString().trim()
                if (nomeExercicio.isNotEmpty()) {
                    adicionarExercicioAFicha(nomeExercicio)
                } else {
                    Toast.makeText(this, "Digite o nome do exercício", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // MÉTODO CORRIGIDO: Usar documentId para criar IDs únicos
    private fun adicionarExercicioAFicha(nomeExercicio: String) {
        val ficha = fichasList.first() // Temos apenas uma ficha

        // Criar novo exercício com uma série padrão
        val novoExercicio = exercicioFun(
            id = "${documentId}_exercicio_${ficha.exercicios.size}", // USAR DOCUMENT ID
            nome = nomeExercicio,
            series = mutableListOf(
                serieFun(
                    id = "${documentId}_ex${ficha.exercicios.size}_serie0", // USAR DOCUMENT ID
                    numero = 1,
                    repeticoes = 12,
                    peso = "",
                    tempo = "60"
                )
            )
        )

        // Adicionar à ficha
        ficha.exercicios.add(novoExercicio)

        Log.d("ADD_EXERCICIO_MAIN", "Exercício '$nomeExercicio' adicionado à ficha ${ficha.letra}")
        Log.d("ADD_EXERCICIO_MAIN", "Total de exercícios agora: ${ficha.exercicios.size}")

        // Notificar que a ficha foi alterada (isso salvará automaticamente)
        onFichaAlterada(ficha, 0)

        // Atualizar o adapter (isso atualizará o contador automaticamente)
        fichaTreinoAdapter.notifyItemChanged(0)

        // Forçar atualização do contador
        fichaTreinoAdapter.atualizarContadores()

        Toast.makeText(this, "Exercício '$nomeExercicio' adicionado!", Toast.LENGTH_SHORT).show()
    }

    // Sobrescrever métodos do ciclo de vida para salvar alterações
    override fun onPause() {
        super.onPause()
        Log.d("LIFECYCLE", "onPause - Salvando alterações pendentes")
        salvarTodasAlteracoesPendentes()
    }

    override fun onStop() {
        super.onStop()
        Log.d("LIFECYCLE", "onStop - Salvando alterações pendentes")
        salvarTodasAlteracoesPendentes()
    }

    override fun onDestroy() {
        Log.d("LIFECYCLE", "onDestroy - Limpando recursos e salvando alterações")

        // Cancelar timer
        saveDebounceTimer?.cancel()

        // Salvar alterações pendentes
        salvarTodasAlteracoesPendentes()

        super.onDestroy()
    }

    // Interceptar botão voltar do sistema
    override fun onBackPressed() {
        Log.d("LIFECYCLE", "onBackPressed - Salvando alterações antes de voltar")
        salvarTodasAlteracoesPendentes()
        super.onBackPressed()
    }

    // Tratamento para quando o usuário minimiza o app
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        Log.d("LIFECYCLE", "onUserLeaveHint - App minimizado, salvando alterações")
        salvarTodasAlteracoesPendentes()
    }

    // Método para salvar todas as alterações pendentes
    private fun salvarTodasAlteracoesPendentes() {
        try {
            // Forçar salvamento de campos que podem estar sendo editados
            if (::fichaTreinoAdapter.isInitialized) {
                fichaTreinoAdapter.salvarTodasAlteracoesPendentes()
            }

            // Se há alterações pendentes, salvar tudo
            if (hasUnsavedChanges) {
                Log.d("SAVE_PENDING", "Há alterações pendentes, salvando...")
                salvarAlteracoesNoFirestore()
            } else {
                Log.d("SAVE_PENDING", "Nenhuma alteração pendente")
            }
        } catch (e: Exception) {
            Log.e("SAVE_PENDING_ERROR", "Erro ao salvar alterações pendentes", e)
        }
    }

    // Método de salvamento existente
    private fun salvarAlteracoesNoFirestore() {
        if (fichasList.isEmpty()) {
            Log.d("FIRESTORE_SAVE", "Nenhuma ficha para salvar")
            return
        }

        val ficha = fichasList.first()
        Log.d("FIRESTORE_SAVE", "Salvando todas as alterações da ficha: ${ficha.letra}")

        salvarFichaAlteradaNoFirestore(ficha)
    }

    // Método adicional para forçar atualização do contador
    private fun atualizarContadorExercicios() {
        if (fichasList.isNotEmpty()) {
            // Forçar atualização do primeiro item (única ficha)
            fichaTreinoAdapter.notifyItemChanged(0)
            fichaTreinoAdapter.atualizarContadores()
        }
    }

    // Método público para forçar recarregamento se necessário
    fun recarregarFicha() {
        carregarFichaEspecificaDoFirestore()
    }
}