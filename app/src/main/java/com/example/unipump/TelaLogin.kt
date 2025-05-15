package com.example.unipump

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore


class TelaLogin : AppCompatActivity() {

    private lateinit var btnVoltar: Button
    private lateinit var textEsqueceuSenha: TextView
    private lateinit var edtEmail: EditText
    private lateinit var edtSenha: EditText
    private lateinit var btnEntrar: AppCompatButton
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore


    // Variável simulando a senha salva
    /*private var senhaSalva: String = "12345" // Senha inicial*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tela_login)

        // Inicializa os componentes da UI
        btnVoltar = findViewById(R.id.btnVoltar)
        textEsqueceuSenha = findViewById(R.id.tvEsqueceuSenha)
        edtEmail = findViewById(R.id.etEmail)
        edtSenha = findViewById(R.id.etSenha)
        btnEntrar = findViewById(R.id.btnEntrar)


        //var auth = Firebase.auth aqui eu j aforneco diretamente a instacia de FirebaseAuth
        auth = FirebaseAuth.getInstance() // aqui eu estou pegando a varivel que declarei la em cima onde disse que ela com o tipo FIrebaseAuth e estou inicializando ela com uma insatncia do FirebaseAuth
        db = Firebase.firestore

        var tipo = intent.getStringExtra("tipo")
        if (tipo == "aluno") {
            edtEmail.hint = "Email ou Telefone"
        } else if (tipo == "funcionario") {
            edtEmail.hint = "Id"
        }

        // Configura os eventos
        configurarEventos()

        // Verifica se há uma nova senha passada pela Intent
        /*val novaSenha = intent.getStringExtra("novaSenha")
        if (novaSenha != null) {
            senhaSalva = novaSenha // Atualiza a senha salva com a nova senha
        }*/
    }

    private fun configurarEventos() {

        val tipo = intent.getStringExtra("tipo")

        btnVoltar.setOnClickListener {
            val intent = Intent(this, TelaInicial::class.java)
            startActivity(intent)
        }

        textEsqueceuSenha.setOnClickListener {
            val intent = Intent(this, TelaEsqueceuSenha::class.java)
            if (tipo == "aluno") {
                intent.putExtra("tipo", "aluno")
            } else if (tipo == "funcionario") {
                intent.putExtra("tipo", "funcionario")
            }
            startActivity(intent)
        }

        // Evento do botão de login
        btnEntrar.setOnClickListener {
            onEntrarClick()
        }
    }

    // Função de login
    private fun onEntrarClick() {
        val usuario = edtEmail.text.toString().trim()
        val senha = edtSenha.text.toString()
        val tipo = intent.getStringExtra("tipo")

        if (usuario.isEmpty() || senha.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            return
        }

        if (tipo == "funcionario" && isValidId(usuario)) {
            // Buscar e-mail associado ao ID no Firestore
            db.collection("funcionarios").document(usuario).get()
                .addOnSuccessListener { documento ->
                    if (documento.exists()) {
                        val email = documento.getString("email")
                        if (!email.isNullOrEmpty()) {
                            loginComEmail(email, senha, tipo)
                        } else {
                            Toast.makeText(this, "ID inválido: e-mail não encontrado", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Funcionário não encontrado", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erro ao buscar funcionário: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                }

        } else if (tipo == "aluno" && (isValidEmail(usuario) || isValidPhone(usuario))) {
            val campoBusca = if (isValidPhone(usuario)) "telefone" else "email"

            db.collection("alunos")
                .whereEqualTo(campoBusca, usuario)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val documento = documents.documents[0]
                        val email = documento.getString("email")
                        if (!email.isNullOrEmpty()) {
                            loginComEmail(email, senha, tipo)
                        } else {
                            Toast.makeText(this, "Email do aluno não encontrado", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Aluno não encontrado", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erro ao buscar aluno: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
        }else {
            Toast.makeText(this, "Formato de login inválido", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loginComEmail(email: String, senha: String, tipo: String?) {
        auth.signInWithEmailAndPassword(email, senha)
            .addOnCompleteListener { autenticacao -> // resultado do login do usuário

                if (autenticacao.isSuccessful) {

                    val uid = auth.currentUser?.uid

                    if (uid != null && tipo != null) {
                        val colecao = if (tipo == "aluno") "alunos" else "funcionarios"

                        db.collection(colecao).whereEqualTo("email", email).get()
                            .addOnSuccessListener { documents ->
                                if (!documents.isEmpty) {
                                    val dadosUsuario = documents.documents[0]

                                    // Obtem SharedPreferences
                                    val prefs = getSharedPreferences(
                                        if (tipo == "aluno") "alunoPrefs" else "funcionarioPrefs",
                                        MODE_PRIVATE
                                    )
                                    val editor = prefs.edit()

                                    // Salva dados que você quiser
                                    editor.putString("uid", uid)
                                    editor.putString("nome", dadosUsuario.getString("nome"))
                                    editor.putString("sobrenome", dadosUsuario.getString("sobrenome"))
                                    editor.putString("idade", dadosUsuario.getString("idade"))
                                    editor.putString("genero", dadosUsuario.getString("genero"))
                                    editor.putString("endereco", dadosUsuario.getString("endereco"))
                                    editor.putString("telefone", dadosUsuario.getString("telefone"))
                                    editor.putString("email", email)
                                    editor.putString("tipo", tipo)
                                    editor.apply() // aplica as mudanças

                                    // Vai para a tela principal
                                    val intent = if (tipo == "aluno") {
                                        Intent(this, TelaPrincipalAluno::class.java)
                                    } else {
                                        Intent(this, TelaFuncionario::class.java)
                                    }
                                    startActivity(intent)
                                    finish()
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(
                                    this,
                                    "Erro ao buscar dados do usuário",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                } else {

                    Toast.makeText(this, "Usuário ou senha inválidos.", Toast.LENGTH_SHORT).show()

                }

            }
            .addOnFailureListener { excecao ->
                val msgErro = when (excecao) {
                    is FirebaseAuthWeakPasswordException ->
                        "Digite uma senha com no mínimo 6 caracteres"
                    is FirebaseAuthInvalidUserException ->
                        "Usuário não encontrado ou desativado"
                    is FirebaseAuthInvalidCredentialsException ->
                        "Credenciais inválidas"
                    else ->
                        "Erro ao autenticar: ${excecao.localizedMessage}"
                }
                Toast.makeText(this, msgErro, Toast.LENGTH_LONG).show()
            }
    }


    override fun onStart() {
        super.onStart()

        val tipo = intent.getStringExtra("tipo")

        val usuarioAtual = FirebaseAuth.getInstance().currentUser

        if (usuarioAtual != null){

            val intent = if (tipo == "aluno") {
                Intent(this, TelaPrincipalAluno::class.java)
            } else {
                Intent(this, TelaFuncionario::class.java)
            }
            startActivity(intent)
            finish()

        }

    }





    private fun isValidEmail(email: String): Boolean {
        // Regex para validar e-mails simples
        val emailPattern = "[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
        return email.matches(emailPattern.toRegex())
    }

    private fun isValidId(id: String): Boolean {
        val idPattern = "^[0-9]+$"  // Regex para validar apenas números
        return id.matches(idPattern.toRegex())
    }

    private fun isValidPhone(phone: String): Boolean {
        // Regex para validar números de telefone (aqui estou considerando números com 10 ou 11 dígitos)
        val phonePattern = "^\\+?[0-9]{10,13}$"
        return phone.matches(phonePattern.toRegex())
    }
}





/*val usuario = edtEmail.text.toString()
    val senha = edtSenha.text.toString()

    val tipo = intent.getStringExtra("tipo") ?: run {
        Toast.makeText(this, "Tipo de usuário não definido.", Toast.LENGTH_SHORT).show()
        return
    }


    // Para alunos: valida se o e-mail ou telefone é válido
    if (isValidEmail(usuario) || isValidPhone(usuario)) {
        // Aqui você deve comparar o e-mail com o que está salvo
        if (usuario == "aluno@exemplo.com" && senha == senhaSalva) {
            val intent = Intent(this, TelaPrincipalAluno::class.java)
            startActivity(intent)
            finish() // Finaliza a tela de login
        } else {
            Toast.makeText(this, "Usuário ou senha inválidos.", Toast.LENGTH_SHORT).show()
        }
    } else {
        // Caso o e-mail/telefone seja inválido
        Toast.makeText(this, "E-mail ou telefone inválido.", Toast.LENGTH_SHORT).show()
    }
    if (isValidId(usuario)){
        if (usuario == "2413103" && senha == senhaSalva) {
            // Verifique se o ID e a senha do funcionário estão corretos
            val intent = Intent(this, TelaFuncionario::class.java)
            startActivity(intent)
            finish() // Finaliza a tela de login
        } else {
            Toast.makeText(this, "ID ou senha inválidos.", Toast.LENGTH_SHORT).show()
        }
    }
}*/