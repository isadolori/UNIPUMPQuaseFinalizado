package com.example.unipump

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.Firebase
import kotlinx.coroutines.launch

class TelaChat : AppCompatActivity() {

    lateinit var setaVoltar: ImageButton
    lateinit var txtResposta: TextView
    lateinit var edtPergunta: EditText
    lateinit var btnEnviarMsg: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tela_chat)

        setaVoltar = findViewById(R.id.SetaVoltar)
        txtResposta = findViewById(R.id.resposta)
        edtPergunta = findViewById(R.id.messageInputLayout)
        btnEnviarMsg = findViewById(R.id.btnEnviarMsg)

        configurarEventos()


    }

    fun configurarEventos() {

        // Definindo o clique do botão de voltar
        setaVoltar.setOnClickListener {
            finish() // Isso chama o comportamento de voltar para a tela anterior
        }

        btnEnviarMsg.setOnClickListener {
            val pergunta = edtPergunta.text.toString().trim()
            if (pergunta.isNotEmpty()) {
                sendPrompt(pergunta)
            } else {
                txtResposta.text = "Digite uma pergunta antes de enviar."
            }
        }



        }

    fun sendPrompt(prompt: String){
        val generativeModel = GenerativeModel(
            modelName = "gemini-2.0-flash" ,
            apiKey = "AIzaSyAYnoacKnW6Dt2Q6Vz9h0Z4hAFmXoV0Aw0"

        )

        lifecycleScope.launch {

            val response = generativeModel.generateContent(prompt)

            response.text?.let { outputContent ->
                Log.d("respost", outputContent)
                txtResposta.text = outputContent.toString()


            }

        }
    }
}