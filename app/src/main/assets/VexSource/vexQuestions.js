
var randnum=null;
randnum = Android.getFromAndroid();


VF = Vex.Flow;

// Create an SVG renderer and attach it to the DIV element named "boo".
var div = document.getElementById("boo")
var renderer = new Vex.Flow.Renderer(document.getElementById("boo"),
                                     Vex.Flow.Renderer.Backends.CANVAS);

// Configure the rendering context.
renderer.resize(500, 500);
var context = renderer.getContext();
context.setFont("Arial", 10, "").setBackgroundFillStyle("#eed");

// Create a stave of width 400 at position 10, 40 on the canvas.
var stave = new VF.Stave(100, 40, 200);

// Add a clef and time signature.
stave.addClef("treble").addTimeSignature("4/4");


// Connect it to the rendering context and draw!
stave.setContext(context).draw();

var notes = [
  // A quarter-note C.
  new VF.StaveNote({clef: "treble", keys: ["c/4"], duration: "h" }),

  // A quarter-note D.
  new VF.StaveNote({clef: "treble", keys: ["d/4"], duration: "h" }),


];

// Create a voice in 4/4 and add above notes
var voice = new VF.Voice({num_beats: 4,  beat_value: 4});
voice.addTickables(notes);

// Format and justify the notes to 400 pixels.
var formatter = new VF.Formatter().joinVoices([voice]).format([voice], 200);

// Render voice
voice.draw(context, stave);

document.write("var here");
document.write(randnum);