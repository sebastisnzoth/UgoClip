# HUGO Clip

**HUGO Clip** é um secretário pessoal por voz para Android. O primeiro MVP usa o próprio celular como hardware: você ativa o Modo HUGO, sai da tela do app e continua falando comandos iniciados por **“Hugo…”**.

> Fale. HUGO cuida da parte digital.

## O que já existe neste MVP

- Serviço Android em primeiro plano com microfone (`foregroundServiceType="microphone"`).
- Reconhecimento de voz em português do Brasil.
- Resposta por voz via TTS.
- Notificação persistente com **Pausar/Retomar** e **Encerrar**.
- Wake prefix: comandos só são executados quando começam com “Hugo”.
- Abrir YouTube, WhatsApp, Spotify, Instagram e Chrome.
- Abrir Maps e pesquisar um lugar por voz.
- Criar notas locais.
- Guardar e recuperar onde um objeto foi deixado.
- Preparar uma mensagem no WhatsApp.
- Criar compromisso para amanhã e abrir o calendário para confirmação.
- Consultar compromissos criados pelo próprio HUGO.
- Memória local sem armazenar arquivos de áudio.

## Teste no celular

1. Abra o projeto no Android Studio.
2. Use JDK 17.
3. Instale Android SDK 36.
4. Compile e instale o app em um Android real.
5. Abra **HUGO Clip**.
6. Autorize o microfone.
7. Toque em **Ativar HUGO**.
8. Saia da tela do app e teste:

```text
Hugo, abre o YouTube
Hugo, abre o Maps para Canasvieiras
Hugo, anota comprar dois disjuntores
Hugo, lembra que deixei a furadeira no armário azul
Hugo, onde deixei a furadeira?
Hugo, prepara uma mensagem dizendo que chego às oito
Hugo, lembra de comprar pão amanhã às 9
Hugo, o que tenho amanhã?
Hugo, pausa
Hugo, encerra
```

## Build

O projeto usa:

- Kotlin
- Jetpack Compose
- Android Gradle Plugin
- Android SpeechRecognizer
- Android TextToSpeech
- Foreground Service
- SharedPreferences/JSON para a memória inicial

Na raiz, com Gradle 8.11.1 instalado:

```bash
gradle :app:assembleDebug
```

O APK fica em:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Também existe GitHub Actions em `.github/workflows/android.yml`; cada push na `main` tenta gerar o APK de debug como artifact **UgoClip-debug**.

## Limitações intencionais do MVP

### Android e tela apagada

O microfone permanece em um **Foreground Service** visível. Android pode interromper/recriar serviços de reconhecimento de voz dependendo do fabricante, economia de bateria e implementação do serviço de voz do aparelho. O código se recupera de timeouts/erros comuns e reinicia o recognizer.

### Abrir outros apps em segundo plano

Android moderno restringe abertura de telas por apps que estão em background. O MVP tenta executar a ação solicitada; em alguns aparelhos/versões, abrir YouTube/Maps diretamente com a tela apagada pode ser bloqueado pelo sistema. A etapa posterior é integrar HUGO como **assistente padrão do Android** (`RoleManager`/`VoiceInteractionService`), que é a rota correta para uma integração profunda.

### WhatsApp

O MVP **prepara** o texto e abre o WhatsApp. Ele não simula cliques e não envia silenciosamente. A pessoa confere contato e conteúdo antes do envio.

### Privacidade

- nenhuma gravação de áudio é salva;
- o Android mostra quando o microfone está ativo;
- ações sensíveis não devem ser silenciosas;
- automação genérica via Accessibility Service não faz parte do núcleo do produto.

## Próximos P0

1. Testar em Android físico com tela ligada/apagada.
2. Ajustar reconexão do SpeechRecognizer por fabricante.
3. Adicionar confirmação por voz antes de mensagens/ligações.
4. Assistant Role + `VoiceInteractionService`.
5. Wake word local de baixa energia.
6. Migrar memória para Room.
7. Integrar o clip Bluetooth físico somente após validar uso diário.

A especificação de produto completa está em [`HUGO_CLIP_MASTER.md`](HUGO_CLIP_MASTER.md).
