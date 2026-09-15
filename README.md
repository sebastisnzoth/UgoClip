# HUGO Clip

HUGO Clip é um assistente pessoal de voz para Android e a base de software do futuro wearable HUGO.

## Estado atual — MVP 0.2

- Foreground Service de microfone com notificação persistente.
- SpeechRecognizer nativo com transcrição parcial e recuperação automática.
- TTS em português do Brasil.
- Orquestrador de comandos por voz.
- Sentinel em três níveis: ações diretas, confirmação e bloqueio.
- Memória persistente com Room.
- Agenda, notas, tarefas, histórico e auditoria de comandos.
- Abertura de apps e Maps por Android Intents.
- Preparação de mensagens e chamadas com confirmação.
- Interface Compose com Voice Orb e simulador visual do futuro clip BLE.
- Cliente Gemini opcional; sem chave configurada, o app continua funcionando com lógica local.

## Teste rápido

1. Instale o APK debug.
2. Abra HUGO Clip e permita o microfone e notificações.
3. Ative o Modo HUGO.
4. Saia da tela do app.
5. Diga: `Hugo, abre o YouTube`.

Outros comandos:

- `Hugo, abre o Maps para Canasvieiras`.
- `Hugo, anota comprar dois disjuntores`.
- `Hugo, lembra que deixei a furadeira no armário azul`.
- `Hugo, onde deixei a furadeira?`.
- `Hugo, o que eu tenho hoje?`.
- `Hugo, pausa`.
- `Hugo, encerra`.

## Gemini opcional

Para habilitar respostas de IA na build, defina `GEMINI_API_KEY` como variável de ambiente antes de compilar. A chave não fica versionada no repositório. Sem chave, o app usa respostas locais e os comandos P0 continuam disponíveis.

## Build

A CI em `.github/workflows/android.yml` gera o APK debug a cada push em `main` e publica o arquivo como artifact `UgoClip-debug`.

Documento de produto: `HUGO_CLIP_MASTER.md`.
