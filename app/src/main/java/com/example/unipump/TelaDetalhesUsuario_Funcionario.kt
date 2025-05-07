package com.example.unipump

import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class TelaDetalhesUsuario_Funcionario : AppCompatActivity() {

    lateinit var btnSetaVoltar: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tela_detalhes_usuario_funcionario)

        btnSetaVoltar = findViewById(R.id.SetaVoltarTelaGerenciamentoAluno)

        btnSetaVoltar.setOnClickListener {
            finish()
        }



    }
}