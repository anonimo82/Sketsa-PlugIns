# Sketsa Links

## Panoramica

**Sketsa Links** è un modulo per Sketsa che aggiunge un pannello dedicato alla creazione e alla modifica di collegamenti SVG. Il plugin lavora direttamente sulla struttura DOM del documento e consente di trasformare un elemento grafico selezionato nel contenuto di un elemento `<a>` senza perdere il nodo originale.

Il progetto è scritto in Java per la piattaforma NetBeans utilizzata da Sketsa e richiede Java 11. Il manifest dichiara il modulo `kiyut.sketsa.modules.links` con specification version **0.1.1**.

## Funzioni principali

- Creazione di un collegamento attorno a un singolo oggetto SVG selezionato.
- Modifica di un collegamento `<a>` già esistente.
- Gestione dei campi **URL**, **Target** e **Title**.
- Scrittura sia di `href` sia di `xlink:href`, per mantenere compatibilità con documenti SVG che usano l'una o l'altra forma.
- Rimozione del collegamento senza eliminare il contenuto grafico racchiuso nel tag `<a>`.
- Riconoscimento automatico del link associato all'oggetto selezionato.
- Integrazione con il sistema di selezione e con il canvas attivo di Sketsa.

## Undo / Redo

La versione 0.1.1 tratta l'aggiornamento di un link esistente come una **sostituzione strutturale del wrapper `<a>`**, preservandone i figli. Questa scelta permette al `DOMUndoManager` di Sketsa di registrare correttamente le modifiche a URL, target e titolo e di ripristinarle tramite Undo/Redo.

Anche la creazione e la rimozione del wrapper vengono eseguite attraverso operazioni DOM compatibili con la cronologia del documento.

## Struttura SVG

Un collegamento creato dal plugin ha, in forma semplificata, questa struttura:

```xml
<a href="https://example.com"
   xlink:href="https://example.com"
   target="_blank"
   title="Example">
    <!-- elemento SVG selezionato -->
</a>
```

Il plugin conserva il contenuto dell'elemento selezionato e, quando un link viene rimosso, reinserisce i nodi figli al posto del wrapper.

## Integrazione con Sketsa

`LinksIntegrator` installa il pannello nell'interfaccia di Sketsa. `Installer` ritenta l'integrazione tramite un timer Swing durante il ripristino del modulo, in modo da agganciarsi all'interfaccia anche quando i componenti dell'applicazione non sono ancora stati costruiti al momento del caricamento iniziale.

## Sorgenti principali

- `src/kiyut/sketsa/modules/links/integration/LinksPanel.java` — interfaccia, gestione selezione e operazioni DOM.
- `src/kiyut/sketsa/modules/links/integration/LinksIntegrator.java` — inserimento del pannello nell'interfaccia Sketsa.
- `src/kiyut/sketsa/modules/links/Installer.java` — inizializzazione del modulo.

## Ambiente di riferimento

- Sketsa 9.1
- NetBeans 11.3
- JDK / Java 11
