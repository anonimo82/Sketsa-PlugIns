# Sketsa Symbols

## Panoramica

**Sketsa Symbols** fornisce a Sketsa strumenti per creare definizioni SVG `<symbol>`, inserire istanze `<use>`, modificarle e trasformarle nuovamente in contenuto SVG indipendente.

Il manifest identifica il modulo come `kiyut.sketsa.modules.symbols` con specification version **0.1.1**. Il progetto è compilato con Java 11.

## Funzioni principali

- Creazione di un `<symbol>` a partire dall'oggetto SVG selezionato.
- Aggiornamento di una definizione `<symbol>` esistente con lo stesso ID.
- Inserimento di nuove istanze `<use>` del simbolo.
- Modifica di simbolo di riferimento e coordinate `x` / `y` di un `<use>` selezionato.
- Distacco di una istanza `<use>` in contenuto SVG concreto, preservando la definizione `<symbol>` originale.
- Creazione automatica di `<defs>` se il documento non ne possiede uno.
- Gestione contemporanea di `href` e `xlink:href`.

## Creazione delle definizioni

Quando viene creato un simbolo, il plugin **clona** l'elemento selezionato invece di spostarlo nel `<defs>`. L'eventuale `id` sul nodo clonato di primo livello viene rimosso per evitare un duplicato immediato con l'oggetto originale.

La definizione risultante segue il modello:

```xml
<defs>
    <symbol id="mySymbol">
        <!-- clone dell'oggetto originale -->
    </symbol>
</defs>
```

## Istanze `<use>`

Le istanze create dal plugin usano entrambe le forme di riferimento:

```xml
<use href="#mySymbol"
     xlink:href="#mySymbol"
     x="0"
     y="0" />
```

Quando una definizione `<symbol>` viene creata, aggiornata, ripristinata con Undo o riapplicata con Redo, il plugin forza anche il refresh delle istanze `<use>` che la referenziano.

Questo comportamento è intenzionale: Batik può mantenere in cache il rendering di una reference già risolta anche quando il DOM della definizione è stato modificato. Riscrivere gli stessi riferimenti invalida quella cache e rende immediatamente visibile il nuovo simbolo.

## Detach

**Detach Use** sostituisce una istanza con contenuto concreto derivato dalla definizione del simbolo. Le coordinate dell'istanza vengono conservate tramite una trasformazione di traslazione quando necessario. La definizione originale in `<defs>` non viene eliminata, quindi altre istanze restano valide.

## Undo / Redo

Il sorgente contiene edit dedicati per:

- modifica dello stato di un `<use>`;
- inserimento di una nuova istanza;
- detach di una istanza;
- creazione o sostituzione della definizione `<symbol>`.

Gli edit della definizione eseguono anche il refresh delle relative istanze dopo Undo e Redo.

## Sorgenti principali

- `src/kiyut/sketsa/modules/symbols/integration/SymbolsPanel.java` — gestione simboli e istanze, detach, refresh Batik e Undo/Redo.
- `src/kiyut/sketsa/modules/symbols/integration/SymbolsIntegrator.java` — integrazione del pannello.
- `src/kiyut/sketsa/modules/symbols/Installer.java` — inizializzazione.

## Ambiente di riferimento

- Sketsa 9.1
- NetBeans 11.3
- Java 11
