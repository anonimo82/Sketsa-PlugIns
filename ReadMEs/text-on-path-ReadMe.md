# Sketsa Text on Path

## Panoramica

**Sketsa Text on Path** aggiunge a Sketsa un pannello per collegare un elemento di testo a un tracciato SVG mediante `<textPath>`, modificare il tracciato di riferimento e il `startOffset`, oppure rimuovere il collegamento mantenendo il contenuto testuale.

Il manifest dichiara il modulo `kiyut.sketsa.modules.textonpath` con specification version **0.1.1** e compilazione Java 11.

## Funzioni principali

- Collegamento di un singolo elemento `<text>` selezionato a un `<path>` identificato da ID.
- Aggiornamento di un `<textPath>` già presente.
- Modifica del valore `startOffset`.
- Scrittura di `href` e `xlink:href` verso il path di riferimento.
- Verifica che l'ID indicato esista davvero e corrisponda a un elemento `<path>`.
- Detach del testo dal tracciato senza perdere nodi o contenuto testuale.
- Riconoscimento automatico di una selezione che si trova già all'interno di un `<textPath>`.

## Struttura SVG

Il collegamento prodotto è standard SVG:

```xml
<text>
    <textPath href="#curve1"
              xlink:href="#curve1"
              startOffset="25%">
        Testo sul tracciato
    </textPath>
</text>
```

Il valore di `startOffset` viene mantenuto come stringa SVG, quindi può rappresentare sia un valore numerico sia una percentuale valida per il documento.

## Strategia di aggiornamento

La versione 0.1.1 usa una strategia a **snapshot completi dell'elemento `<text>`**. Per Attach, Update e Detach viene costruita la nuova struttura, quindi il nodo `<text>` corrente viene sostituito con la nuova versione.

Questa scelta evita che una singola operazione logica venga frammentata in numerose piccole modifiche DOM e risolve sia l'aggiornamento visivo del `startOffset` sia l'isolamento delle operazioni Undo/Redo.

## Undo / Redo

`TextSnapshotEdit` conserva lo stato precedente e quello successivo dell'intero `<text>`. L'edit viene inserito esplicitamente nella entry corrente del `DOMUndoManager`, così Attach/Update/Detach risultano ciascuno come un'unica operazione reversibile.

Dopo la sostituzione il plugin aggiorna anche il riferimento all'elemento vivo e prova a ristabilirne la selezione sul canvas.

## Sorgenti principali

- `src/kiyut/sketsa/modules/textonpath/integration/TextOnPathPanel.java` — UI, risoluzione del path, snapshot del testo e Undo/Redo.
- `src/kiyut/sketsa/modules/textonpath/integration/TextOnPathIntegrator.java` — integrazione del pannello.
- `src/kiyut/sketsa/modules/textonpath/Installer.java` — inizializzazione.

## Ambiente di riferimento

- Sketsa 9.1
- NetBeans 11.3
- Java 11
