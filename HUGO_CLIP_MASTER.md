# HUGO CLIP — MD MAESTRO
## Assistente de voz pessoal para Android + futuro wearable

**Versão:** 0.1 MVP  
**Objetivo:** transformar o celular em um “secretário de voz” que continua disponível mesmo com a tela fechada ou enquanto o usuário está em outros apps, e depois conectar o mesmo cérebro a um clip/wearable simples.

---

## 1. VISÃO DO PRODUTO

**HUGO Clip não é um gadget. É um secretário pessoal por voz.**

A promessa é simples:

> **Você fala. HUGO entende, organiza e executa — com confirmação quando a ação for sensível.**

O usuário não precisa desbloquear o celular, procurar um app, abrir uma tela, digitar ou navegar por menus para tarefas simples.

Exemplos:

- “Hugo, abre o YouTube.”
- “Hugo, lembra de cobrar o João quinta às nove.”
- “Hugo, anota: comprar dois disjuntores.”
- “Hugo, abre o Maps para Canasvieiras.”
- “Hugo, prepara uma mensagem para o Ariel: chego às oito.”
- “Hugo, o que eu tenho hoje?”
- “Hugo, lê meus próximos compromissos.”
- “Hugo, adiciona reunião amanhã às 15.”
- “Hugo, guarda esta ideia.”
- “Hugo, me lembra onde deixei a furadeira.”

A diferença para um app tradicional é **reduzir a fricção a quase zero**.

---

## 2. MVP QUE DEVEMOS TESTAR PRIMEIRO

### Meta

Construir primeiro **somente o app Android**, sem hardware.

O telefone será o cérebro e também o microfone.

### Fluxo principal

1. Usuário abre o app.
2. Ativa **MODO HUGO**.
3. O app solicita permissões necessárias.
4. Um serviço de microfone permanece ativo em primeiro plano.
5. O Android mostra uma notificação persistente indicando que o microfone está sendo usado.
6. O usuário pode sair do app ou apagar a tela.
7. Enquanto o MODO HUGO estiver ativo, o sistema continua escutando.
8. Ao detectar um comando, converte voz em intenção.
9. O roteador decide qual ação executar.
10. Ações sensíveis pedem confirmação.
11. HUGO responde por voz.
12. O usuário pode dizer “Hugo, parar” ou tocar em **Desativar HUGO**.

### Regra

**Nunca esconder que o microfone está ativo.**

---

## 3. LIMITAÇÃO REAL DO ANDROID

Em Android moderno, um app comum não pode simplesmente ligar o microfone escondido para sempre.

Para o MVP:

- iniciar o serviço de microfone enquanto o app está visível;
- usar um **Foreground Service** do tipo `microphone`;
- manter notificação persistente;
- exigir `RECORD_AUDIO`;
- permitir ao usuário encerrar a sessão facilmente.

No Android 14+, iniciar esse serviço do zero quando o app já está totalmente em segundo plano é restringido.

Por isso o primeiro fluxo será:

**Abrir HUGO → Ativar Modo HUGO → sair do app → continuar usando por voz.**

Mais adiante, para integração mais profunda, HUGO poderá pedir ao usuário para configurá-lo como **assistente padrão do Android**, usando o papel oficial de Assistant / `VoiceInteractionService`.

---

## 4. ARQUITETURA DO MVP

```text
┌─────────────────────────────┐
│        MICROFONE            │
│ Foreground Voice Service    │
└──────────────┬──────────────┘
               │ áudio
               ▼
┌─────────────────────────────┐
│       SPEECH ENGINE         │
│ STT / detecção de fala      │
└──────────────┬──────────────┘
               │ texto
               ▼
┌─────────────────────────────┐
│      HUGO ORCHESTRATOR      │
│ entende intenção + contexto │
└──────────────┬──────────────┘
               │
        ┌──────┴──────┐
        ▼             ▼
┌──────────────┐  ┌───────────────┐
│ ACTION ROUTER│  │ HUGO MEMORY   │
│ apps/agenda  │  │ notas/tarefas │
└──────┬───────┘  └───────────────┘
       │
       ▼
┌─────────────────────────────┐
│        SENTINEL             │
│ segurança + confirmações    │
└──────────────┬──────────────┘
               │ ação permitida
               ▼
┌─────────────────────────────┐
│      ANDROID / APIS         │
│ Apps · Calendar · Maps etc. │
└──────────────┬──────────────┘
               │
               ▼
┌─────────────────────────────┐
│            TTS              │
│       resposta por voz      │
└─────────────────────────────┘
```

---

## 5. COMPONENTES

### 5.1 Voice Foreground Service

Responsável por:

- manter sessão de voz ativa;
- capturar áudio;
- detectar início/fim da fala;
- mostrar estado na notificação;
- parar imediatamente quando solicitado.

Estados:

```text
OFF
STARTING
LISTENING
PROCESSING
SPEAKING
PAUSED
ERROR
```

### 5.2 Speech Engine

Responsável por converter fala em texto.

Para o MVP, o sistema deve ser substituível:

```text
SpeechProvider
 ├── AndroidSpeechProvider
 ├── CloudSpeechProvider
 └── LocalSpeechProvider (futuro)
```

Objetivo: evitar que o produto fique preso a um único fornecedor.

### 5.3 HUGO Orchestrator

É o cérebro.

Entrada:

```json
{
  "transcript": "Hugo abre o YouTube",
  "context": {},
  "device_state": {}
}
```

Saída:

```json
{
  "intent": "OPEN_APP",
  "target": "youtube",
  "requires_confirmation": false
}
```

Outro exemplo:

```json
{
  "intent": "SEND_MESSAGE",
  "target": "Ariel",
  "content": "Chego às oito",
  "requires_confirmation": true
}
```

---

## 6. ROTEADOR DE AÇÕES

### P0 — deve funcionar no primeiro MVP

- abrir aplicativo;
- abrir site;
- abrir YouTube;
- abrir Maps;
- criar lembrete;
- criar nota;
- adicionar tarefa;
- criar evento no calendário;
- consultar agenda;
- iniciar ligação após confirmação;
- preparar mensagem;
- responder por voz;
- perguntar “o que tenho hoje?”;
- armazenar memória simples;
- ativar/desativar HUGO.

### P1

- Spotify/música;
- navegação passo a passo;
- leitura de notificações com permissão explícita;
- respostas rápidas a notificações quando a API do app permitir;
- contatos favoritos;
- rotinas;
- modo trabalho;
- modo carro;
- localização contextual;
- integração com wearable Bluetooth.

### P2

- wake word local;
- papel de assistente padrão do Android;
- memória semântica;
- resumo automático do dia;
- automações entre apps usando APIs oficiais;
- versão HUGO Clip física.

---

## 7. WHATSAPP — COMO FAZER CORRETAMENTE

No MVP, HUGO deve fazer:

> “Hugo, prepara uma mensagem para João dizendo que chego às oito.”

Fluxo:

1. localizar contato;
2. gerar o texto;
3. confirmar:
   - “Enviar para João: ‘Chego às oito’. Confirmar?”
4. usuário responde “sim”;
5. abrir o fluxo oficial disponível no Android/WhatsApp.

Não depender de “clicar sozinho” na interface do WhatsApp usando Accessibility Service.

Para automação comercial real e suportada, usar a API oficial do WhatsApp Business quando aplicável.

---

## 8. NÃO USAR ACCESSIBILITY COMO ATALHO

Não construir o núcleo do produto simulando cliques em qualquer app via Accessibility Service.

Motivos:

- fragilidade;
- telas mudam;
- risco de segurança;
- forte restrição de política do Google Play;
- automação autônoma genérica por Accessibility pode ser rejeitada.

Preferir:

1. Android Intents;
2. APIs oficiais do Android;
3. APIs públicas dos apps;
4. Assistant Role;
5. Notification APIs autorizadas;
6. integrações específicas.

---

## 9. SENTINEL — CAMADA DE SEGURANÇA

Toda ação passa pelo **Sentinel**.

### Nível A — executar sem confirmação

- abrir app;
- abrir mapa;
- criar nota;
- consultar agenda;
- ler uma nota;
- controlar reprodução local.

### Nível B — pedir confirmação

- mandar mensagem;
- fazer ligação;
- criar/alterar compromisso;
- apagar informação;
- compartilhar arquivo;
- publicar conteúdo.

### Nível C — bloquear ou exigir fluxo especial

- movimentação financeira;
- senha;
- alteração de segurança;
- instalação silenciosa;
- permissões críticas;
- exclusão em massa;
- comandos potencialmente perigosos.

Regra central:

> **IA propõe. Sentinel autoriza. Android executa.**

---

## 10. MEMÓRIA DO HUGO

Banco local inicial: **Room / SQLite**.

Entidades:

```text
Note
Task
Reminder
PersonAlias
Preference
CommandHistory
MemoryItem
Routine
```

Exemplo:

Usuário:
> “Hugo, lembra que deixei a furadeira no armário azul.”

Memória:

```json
{
  "type": "OBJECT_LOCATION",
  "subject": "furadeira",
  "value": "armário azul",
  "created_at": "..."
}
```

Depois:

> “Hugo, onde está a furadeira?”

Resposta:

> “Você me disse que deixou no armário azul.”

---

## 11. INTERFACE

A interface visual deve ser mínima.

### Tela principal

```text
              HUGO

             ◉
        ESCUTANDO

   “Fale normalmente”

     [ Pausar HUGO ]

────────────────────────
Hoje
• 09:00 reunião
• cobrar João
• comprar disjuntores
────────────────────────

Memória   Agenda   Histórico   Config.
```

### Notificação persistente

```text
HUGO está escutando
● Microfone ativo

[Pausar] [Falar] [Encerrar]
```

---

## 12. PRIVACIDADE

Padrão recomendado:

- áudio não armazenado por padrão;
- indicador visual sempre que microfone estiver ativo;
- botão claro para apagar histórico;
- memória separada do áudio;
- criptografia local para informações pessoais;
- permissões pedidas somente quando necessárias;
- nenhuma ação silenciosa em apps de terceiros;
- log visível de ações executadas.

---

## 13. STACK RECOMENDADO

### Android

- Kotlin
- Jetpack Compose
- Coroutines / Flow
- Foreground Service
- Room
- WorkManager
- Android Intents
- RoleManager / Assistant Role (fase posterior)
- VoiceInteractionService (fase posterior)

### IA

Criar camada abstrata:

```text
AIProvider
```

Assim podemos alternar entre:

- OpenAI;
- Gemini;
- modelo local;
- outro serviço.

### Voz

```text
SpeechToTextProvider
TextToSpeechProvider
```

TTS inicial pode usar o mecanismo nativo do Android.

---

## 14. ESTRUTURA DE PROJETO

```text
hugo-clip/
├── app/
├── core/
│   ├── voice/
│   ├── ai/
│   ├── actions/
│   ├── memory/
│   ├── security/
│   └── contacts/
├── features/
│   ├── home/
│   ├── agenda/
│   ├── memories/
│   ├── history/
│   └── settings/
├── services/
│   └── HugoVoiceService.kt
├── docs/
│   ├── HUGO_CLIP_MASTER.md
│   ├── SECURITY.md
│   └── COMMANDS.md
└── README.md
```

---

## 15. COMANDOS DE ACEITAÇÃO DO MVP

Antes de considerar o MVP pronto, estes comandos devem funcionar em um Android real:

```text
Hugo, abre o YouTube.
Hugo, abre o Maps.
Hugo, lembra de comprar pão amanhã às nove.
Hugo, o que eu tenho amanhã?
Hugo, anota “ligar para Ariel”.
Hugo, onde deixei a furadeira?
Hugo, prepara uma mensagem para João.
Hugo, pausa.
Hugo, volta a escutar.
Hugo, encerra.
```

E devem continuar funcionando após sair da tela principal enquanto o **Modo HUGO** estiver ativo.

---

## 16. ROADMAP

### Sprint 0 — fundação

- projeto Android;
- Compose;
- permissões;
- foreground microphone service;
- notificação persistente;
- STT;
- TTS.

### Sprint 1 — cérebro

- parser de comandos;
- AI Provider;
- Action Router;
- Sentinel;
- logs;
- fallback quando não entender.

### Sprint 2 — utilidade real

- abrir apps;
- notas;
- tarefas;
- lembretes;
- calendário;
- Maps;
- contatos;
- mensagem preparada.

### Sprint 3 — memória

- Room;
- busca;
- memória contextual;
- histórico.

### Sprint 4 — uso diário

- bateria;
- estabilidade;
- tela apagada;
- Bluetooth;
- interrupções;
- tratamento de chamadas;
- recuperação após erro.

### Sprint 5 — produto

- Assistant Role;
- VoiceInteractionService;
- wake word local;
- wearable.

---

## 17. HUGO CLIP — HARDWARE FUTURO

Somente depois de validar o app.

Hardware mínimo:

- microfone MEMS;
- botão físico;
- LED;
- buzzer ou vibração;
- Bluetooth Low Energy;
- bateria pequena;
- USB-C;
- microcontrolador BLE.

O clip **não precisa rodar a IA**.

Fluxo:

```text
CLIP
  ↓ Bluetooth
CELULAR
  ↓
HUGO APP
  ↓
IA / AÇÃO
  ↓
RESPOSTA
```

Isso reduz custo, peso e consumo.

---

## 18. TESTE QUE DECIDE SE O PRODUTO EXISTE

Dar o app a pessoas reais por alguns dias.

Pergunta principal:

> “Você passou a fazer alguma coisa com HUGO que antes não fazia porque dava preguiça de abrir o celular?”

Se a resposta for **sim**, existe produto.

Se a resposta for:

> “É legal, mas eu faço igual no celular.”

então ainda é gadget e precisamos mudar.

---

## 19. POSICIONAMENTO

Não vender como:

> “wearable com inteligência artificial”.

Vender como:

> **HUGO — seu secretário de voz.**

ou

> **HUGO — sua segunda memória.**

ou

> **Fale. HUGO cuida do resto.**

---

## 20. REGRA MESTRA

> **O produto só existe se economizar ações.**

Se uma tarefa exigir mais toques, confirmações e telas com HUGO do que sem HUGO, o fluxo está errado.

**Objetivo final:**
uma pessoa falar naturalmente e o sistema resolver a parte digital sem obrigá-la a parar o que está fazendo.
