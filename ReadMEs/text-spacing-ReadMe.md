# Sketsa Text Spacing

## Panoramica

**Sketsa Text Spacing** estende gli strumenti di stile del testo di Sketsa con controlli indipendenti per `letter-spacing` e `word-spacing` SVG/CSS.

Il modulo `kiyut.sketsa.modules.textspacing` ha specification version **0.3.4** ed è configurato per Java 11.

## Funzioni principali

- Regolazione di **Letter spacing** in pixel.
- Regolazione di **Word spacing** in pixel.
- Intervallo dei controlli da `-1000` a `1000`, con passo `0.5`.
- Applicazione immediata alla singola selezione testuale corrente.
- Supporto sia allo stile CSS sia agli attributi di presentazione SVG.
- Refresh esplicito del canvas dopo la modifica per aggiornare immediatamente il rendering Batik/Sketsa.
- Integrazione nel componente nativo **Text Style** invece di creare un editor separato.

## Gestione della selezione e del focus

Il pannello conserva l'ultimo elemento di testo valido quando il focus passa dal documento ai controlli del plugin. Questo evita che l'editor si disattivi soltanto perché il global lookup di NetBeans diventa temporaneamente vuoto mentre si interagisce con uno spinner.

I pulsanti freccia degli spinner sono inoltre resi non focalizzabili, riducendo le interferenze con il focus del documento SVG.

## Formati supportati

`DOMUtilities.updateProperty()` può scrivere una proprietà secondo le preferenze di formattazione di Sketsa:

- nello `style` CSS dell'elemento;
- come attributo di presentazione SVG.

Il plugin legge entrambe le forme, per cui una riselezione del testo non azzera i controlli quando il documento usa un formato diverso da quello atteso.

Un valore zero viene scritto come assenza della proprietà; valori interi e decimali vengono serializzati in `px`.

## Undo / Redo

L'applicazione di letter-spacing e word-spacing viene racchiusa in una singola transazione `DOMUndoManager` denominata **Text Spacing**, così le due proprietà vengono annullate o ripristinate insieme.

## Aggiornamento visivo

La versione 0.3.4 richiama esplicitamente `VectorCanvas.refresh()` dopo `DOMUtilities.updateProperty()`. Questo forza Sketsa/Batik a ricostruire il testo renderizzato subito dopo la modifica invece di attendere un successivo evento di repaint o selezione.

## Sorgenti principali

- `src/kiyut/sketsa/modules/textspacing/integration/TextSpacingPanel.java` — controlli di spaziatura, lettura/scrittura proprietà e Undo/Redo.
- `src/kiyut/sketsa/modules/textspacing/integration/TextStyleIntegrator.java` — integrazione nel pannello Text Style.
- `src/kiyut/sketsa/modules/textspacing/Installer.java` — inizializzazione.

## Ambiente di riferimento

- Sketsa 9.1
- NetBeans 11.3
- Java 11
