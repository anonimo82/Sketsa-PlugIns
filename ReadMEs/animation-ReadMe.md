# Sketsa Animation Editor

## Panoramica

**Sketsa Animation Editor** è il progetto più esteso dell'archivio. Fornisce a Sketsa un editor visuale a tracce e keyframe per animazioni SVG/SMIL, con timeline, inspector, riproduzione/scrubbing e anteprima live sul canvas.

Il codice sorgente identifica il modulo come **Animation Editor 1.6.11 – M5 Multi Object Linked Edit Fix**. Il `manifest.mf` contiene invece `OpenIDE-Module-Specification-Version: 1.6.8`; i due numeri sono quindi riportati separatamente perché appartengono a metadata differenti presenti nel progetto.

Il modulo usa Java 11 e si integra come finestra NetBeans `TopComponent` denominata **Animation Editor - SMIL**.

## Modello di editing

L'editor rappresenta le animazioni come:

- oggetti SVG;
- tracce SMIL espandibili;
- keyframe con tempo e valore;
- playhead temporale;
- inspector per proprietà, timing e composizione.

`Timeline`, `TimelineModel` e `SMILTrack` separano la rappresentazione della timeline dalla logica DOM e dall'anteprima.

## Tipi di traccia authorabili

Il menu **Add Track** supporta direttamente:

### Geometria

- `x`
- `y`
- `cx`
- `cy`
- `r`
- `width`
- `height`
- path `d`

### Aspetto

- `opacity`
- `fill`
- `fill-opacity`
- `stroke`
- `stroke-opacity`
- `stroke-width`

### Stato

- `visibility`
- `set` per `x`, `y`, `opacity`, `fill`, `visibility`

### Trasformazioni

- `translate`
- `scale`
- `rotate`
- `skewX`
- `skewY`

### Motion

- `animateMotion` mediante **Motion Path**;
- riferimento a un path tramite `<mpath>` e `href` / `xlink:href`;
- path SVG inline direttamente nell'attributo `path`;
- controllo della rotazione del moto;
- gestione dell'anchor del motion.

### Tracce generiche

- proprietà numeriche;
- proprietà colore;
- proprietà discrete.

Queste permettono di animare attributi non esposti come preset dedicati, mantenendo il modello SVG/SMIL standard.

## Timing e composizione SMIL

L'inspector gestisce le principali proprietà temporali e compositive, tra cui:

- `begin`;
- `end`;
- durata;
- `repeatCount`;
- `repeatDur`;
- `restart`;
- `fill` (`freeze` / `remove`);
- `calcMode`, inclusi `linear`, `discrete`, `paced` e `spline`;
- `keySplines`;
- `additive` (`replace` / `sum`);
- `accumulate` (`none` / `sum`).

Il codice contiene inoltre parsing e risoluzione di espressioni temporali basate su clock, eventi e syncbase, oltre alla gestione di animazioni con durata o ripetizione indefinite.

## Keyframe e interpolazione

L'editor permette di aggiungere, spostare e rimuovere keyframe e valuta le tracce durante scrub e playback. Il runtime interno comprende logica per:

- interpolazione numerica;
- interpolazione colore;
- `calcMode="discrete"`;
- easing spline cubico;
- tempi `paced` per trasformazioni;
- morph di path `d` quando la topologia è compatibile;
- composizione di valori additive/accumulate dove prevista.

## Trasformazioni e pivot

Per `rotate`, `scale`, `skewX` e `skewY`, le trasformazioni create dall'editor usano come pivot predefinito il **centro visuale locale dell'oggetto**.

Per mantenere il risultato portabile come SVG/SMIL standard, scale e skew possono essere accompagnati da `animateTransform` helper che realizzano la sequenza di traslazione al pivot, trasformazione e traslazione inversa. Gli helper restano nascosti dalla timeline, condividono il timing della traccia principale e vengono eliminati insieme ad essa.

`translate` non necessita di pivot.

## Motion Path

Il motion authoring accetta:

- `#id` o `id` di un path esistente, generando/aggiornando `<mpath>`;
- dati SVG path inline che iniziano con un comando di path, salvati nell'attributo `path` di `<animateMotion>`.

Il codice mantiene anche la compatibilità tra `href` e `xlink:href` per i riferimenti `<mpath>`.

## Multi-object authoring

Quando sul canvas sono selezionati più oggetti, una nuova traccia può essere applicata a tutti i target in un'unica operazione nativa di Undo. La prima selezione viene quindi usata come riferimento per l'editing della timeline, mentre le tracce correlate possono essere mantenute sincronizzate durante l'authoring multi-oggetto.

## Anteprima e playback

L'editor non si limita a modificare il DOM. Include un runtime di anteprima che aggiorna direttamente lo stato visuale Batik durante scrub e playback, con supporto per:

- trasformazioni e motion;
- geometria e path;
- opacity e visibility;
- fill e stroke;
- proprietà generiche;
- eventi SMIL;
- playback con estensione della timeline quando necessario.

Sono presenti controlli Play, Pause, Stop e Zoom della timeline.

## Ripristino dello stato statico

Una parte importante del codice gestisce la cancellazione di una traccia attiva. Dopo la rimozione, l'editor ripristina immediatamente lo stato SVG statico/autoriale dell'oggetto e rivaluta le tracce rimaste al tempo corrente.

Il ripristino copre trasformazioni, motion, geometria, path, opacity, visibility, fill/stroke e proprietà generiche, evitando che uno stato di preview Batik rimanga visivamente “bloccato” dopo la cancellazione della relativa animazione.

## Undo / Redo e persistenza

Le operazioni di authoring sono integrate con l'Undo/Redo di Sketsa. Le animazioni create sono normali elementi SVG/SMIL (`<animate>`, `<animateTransform>`, `<animateMotion>`, `<set>`, `<mpath>`), quindi rimangono nel documento e possono essere salvate e riaperte senza dipendere da uno stato runtime proprietario dell'anteprima.

## Componenti principali

- `AnimationTopComponent.java` — finestra NetBeans, collegamento al documento e Undo/Redo globale.
- `AnimationEditor.java` — UI principale, authoring, inspector, preview e playback.
- `Timeline.java` — vista e interazione della timeline.
- `TimelineModel.java` — modello ad albero di oggetti/tracce.
- `SMILTrack.java` — astrazione di una traccia SMIL e dei relativi attributi/valori.
- `TimingCellEditor.java`, `TimingCellRenderer.java`, `TimingHeaderRenderer.java`, `TimingValue.java` — editing e rendering del timing.
- `NameCellRenderer.java` — rendering dei nomi nella timeline.

## Test presenti nel progetto

Il progetto include una suite di regressione M5 (`TESTS-1.6.11-M5-FULL-AUTHORING-EXPANSION.txt`) che copre:

- trasformazioni;
- visibility;
- morph `d`;
- proprietà numeriche, colore e discrete;
- timing/composizione avanzati;
- motion referenziato e inline;
- authoring multi-oggetto;
- portabilità SVG/SMIL;
- regressioni delle milestone precedenti e casi reali di animazione.

Le note fix-only documentano inoltre correzioni specifiche per cancellazione delle tracce, refresh dello stroke, pivot delle trasformazioni, visibility e motion authoring.

## Ambiente di riferimento

- Sketsa
- NetBeans Platform
- Java 11
- SVG + SMIL
