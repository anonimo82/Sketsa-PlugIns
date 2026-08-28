(function(){
'use strict';
var ir=window.__SketsaAudioTreeIR||{valid:false,nodes:[],connections:[],modulations:[],automations:[],events:[],sidechains:[],errors:['Missing Audio Tree IR'],warnings:[]};
var ctx=null,nodes={},sends=[],sources=[],modulations=[],automations=[],eventBindings=[],sidechains=[],eventDeliveries=[],started=false,runtimeWarnings=[],busBound=false;
function ensureContext(){if(!ctx){var C=window.AudioContext||window.webkitAudioContext;if(!C)throw new Error('Web Audio API unavailable');ctx=new C();}return ctx;}
function p(n,k,f){return n.params&&n.params[k]!=null?n.params[k]:f;}
function makeImpulse(c,duration,decay){var len=Math.max(1,Math.floor(c.sampleRate*duration)),buf=c.createBuffer(2,len,c.sampleRate);for(var ch=0;ch<2;ch++){var data=buf.getChannelData(ch);for(var i=0;i<len;i++){var env=Math.pow(1-i/len,decay);data[i]=(Math.random()*2-1)*env;}}return buf;}
function createNode(c,n){
  var t=n.type,entry={info:n,node:null,runtimeKind:'GainNode',controlActive:true};
  if(t==='source'){var out=c.createGain(),osc=c.createOscillator();out.gain.value=p(n,'level',0.05);osc.type=p(n,'waveform','sine');osc.frequency.value=p(n,'frequency',440);osc.connect(out);osc.start();entry.node=out;entry.source=osc;entry.runtimeKind='OscillatorNode';entry.nominalLevel=p(n,'level',0.05);sources.push(entry);return entry;}
  if(t==='gain'){var g=c.createGain();g.gain.value=p(n,'gain',1);entry.node=g;entry.runtimeKind='GainNode';return entry;}
  if(t==='return'){var rg=c.createGain();rg.gain.value=p(n,'gain',1);entry.node=rg;entry.runtimeKind='GainNode(Return)';return entry;}
  if(t==='pan'){if(c.createStereoPanner){var sp=c.createStereoPanner();sp.pan.value=p(n,'pan',0);entry.node=sp;entry.runtimeKind='StereoPannerNode';}else{var pg=c.createGain();entry.node=pg;entry.runtimeKind='GainNode(fallback)';runtimeWarnings.push('StereoPannerNode unavailable; '+n.id+' uses pass-through gain.');}return entry;}
  if(t==='filter'){var f=c.createBiquadFilter();f.type=p(n,'filterType','lowpass');f.frequency.value=p(n,'frequency',1200);f.Q.value=p(n,'q',0.707);f.gain.value=p(n,'gain',0);entry.node=f;entry.runtimeKind='BiquadFilterNode';return entry;}
  if(t==='compressor'){var d=c.createDynamicsCompressor();d.threshold.value=p(n,'threshold',-24);d.knee.value=p(n,'knee',30);d.ratio.value=p(n,'ratio',12);d.attack.value=p(n,'attack',0.003);d.release.value=p(n,'release',0.25);entry.node=d;entry.runtimeKind='DynamicsCompressorNode';return entry;}
  if(t==='analyser'){var a=c.createAnalyser();a.fftSize=p(n,'fftSize',2048);a.smoothingTimeConstant=p(n,'smoothing',0.8);entry.node=a;entry.runtimeKind='AnalyserNode';return entry;}
  if(t==='delay'){var dl=c.createDelay(10);dl.delayTime.value=p(n,'delayTime',0.28);entry.node=dl;entry.runtimeKind='DelayNode';return entry;}
  if(t==='reverb'){var cv=c.createConvolver();cv.normalize=true;cv.buffer=makeImpulse(c,p(n,'duration',1.5),p(n,'decay',2));entry.node=cv;entry.runtimeKind='ConvolverNode';return entry;}
  if(t==='lfo'){var lo=c.createOscillator(),lg=c.createGain();lo.type=p(n,'waveform','sine');lo.frequency.value=p(n,'frequency',2);lg.gain.value=p(n,'depth',0.25);lo.connect(lg);lo.start();entry.node=lg;entry.source=lo;entry.runtimeKind='LFO(OscillatorNode)';return entry;}
  if(t==='automation'){var ag=c.createGain();ag.gain.value=1;entry.node=ag;entry.runtimeKind='AutomationController';return entry;}
  var pass=c.createGain();pass.gain.value=1;entry.node=pass;entry.runtimeKind='GainNode(pass-through)';return entry;
}
function targetParam(entry,name){if(!entry||!entry.node)return null;var n=entry.node;if(name==='gain'&&n.gain)return n.gain;if(name==='pan'&&n.pan)return n.pan;if(name==='frequency'&&n.frequency)return n.frequency;if((name==='q'||name==='Q')&&n.Q)return n.Q;if(name==='delayTime'&&n.delayTime)return n.delayTime;if(name==='threshold'&&n.threshold)return n.threshold;if(name==='knee'&&n.knee)return n.knee;if(name==='ratio'&&n.ratio)return n.ratio;if(name==='attack'&&n.attack)return n.attack;if(name==='release'&&n.release)return n.release;return null;}
function applyAutomation(a){var target=nodes[a.target],param=targetParam(target,a.param);if(!param){runtimeWarnings.push('Automation target parameter not found: '+a.target+'.'+a.param);return false;}var raw=String(a.curve||'');var pts=[];raw.split(',').forEach(function(piece){var x=piece.trim().split(':');if(x.length!==2)return;var t=Number(x[0]),v=Number(x[1]);if(isFinite(t)&&isFinite(v)&&t>=0)pts.push({t:t,v:v});});pts.sort(function(a,b){return a.t-b.t;});if(!pts.length){runtimeWarnings.push('Automation curve has no valid points: '+a.id);return false;}var base=ctx.currentTime;param.cancelScheduledValues(base);param.setValueAtTime(pts[0].v,base+pts[0].t);for(var i=1;i<pts.length;i++)param.linearRampToValueAtTime(pts[i].v,base+pts[i].t);automations.push({id:a.id,target:a.target,param:a.param,curve:raw,points:pts.length,referenceId:a.referenceId});return true;}
function mappedValue(binding,value){var v=Number(value);if(!isFinite(v))return null;v=v*(typeof binding.scale==='number'?binding.scale:1)+(typeof binding.offset==='number'?binding.offset:0);if(typeof binding.min==='number')v=Math.max(binding.min,v);if(typeof binding.max==='number')v=Math.min(binding.max,v);return v;}
function applyRuntimeBinding(binding,detail){
  var entry=nodes[binding.target],ok=false,mapped=null;
  if(!entry)return false;
  if(binding.action==='set-param'){
    var param=targetParam(entry,binding.param);mapped=mappedValue(binding,detail.value);if(param&&mapped!==null){if(param.setValueAtTime&&ctx)param.setValueAtTime(mapped,ctx.currentTime);else param.value=mapped;ok=true;}
  }else if(binding.action==='stop'){
    if(entry.info.type==='source'&&entry.node.gain){entry.node.gain.value=0;entry.controlActive=false;ok=true;}
  }else if(binding.action==='start'||binding.action==='trigger'){
    if(entry.info.type==='source'&&entry.node.gain){entry.node.gain.value=entry.nominalLevel==null?1:entry.nominalLevel;entry.controlActive=true;ok=true;}
  }else if(binding.action==='toggle'){
    if(entry.info.type==='source'&&entry.node.gain){entry.controlActive=!entry.controlActive;entry.node.gain.value=entry.controlActive?(entry.nominalLevel==null?1:entry.nominalLevel):0;ok=true;}
  }
  eventDeliveries.push({id:binding.id,event:binding.event,action:binding.action,target:binding.target,param:binding.param||'',value:detail.value,mappedValue:mapped,applied:ok});
  if(eventDeliveries.length>64)eventDeliveries.shift();
  return ok;
}
function handleRuntimeEvent(ev){var d=ev&&ev.detail||{};if(d.__sketsaAudioTreeInternal)return;var name=String(d.name||'');if(!name)return;eventBindings.forEach(function(b){if(b.event===name)applyRuntimeBinding(b,d);});}
function bindRuntimeBus(){if(busBound)return;busBound=true;document.addEventListener('sketsa:runtime:event',handleRuntimeEvent);}
function emitRuntimeEvent(payload){var d=Object.assign({},payload||{});if(!d.source)d.source='custom';document.dispatchEvent(new CustomEvent('sketsa:runtime:event',{detail:d}));return d;}
function build(){
  if(started)return snapshot();started=true;
  if(!ir.valid){emit('error',{errors:ir.errors.slice()});return snapshot();}
  try{
    var c=ensureContext();
    ir.nodes.forEach(function(n){nodes[n.id]=createNode(c,n);});
    ir.connections.forEach(function(x){var a=nodes[x.from],b=nodes[x.to];if(!a||!b)return;if(x.role==='send'){var sg=c.createGain();sg.gain.value=typeof x.amount==='number'?x.amount:1;a.node.connect(sg);sg.connect(b.node);sends.push({from:x.from,to:x.to,amount:sg.gain.value,referenceId:x.referenceId,node:sg});}else a.node.connect(b.node);});
    (ir.modulations||[]).forEach(function(m){var from=nodes[m.from],to=nodes[m.to],param=targetParam(to,m.param);if(!from||!param){runtimeWarnings.push('Modulation target unavailable: '+m.from+' -> '+m.to+'.'+m.param);return;}var mg=c.createGain();mg.gain.value=typeof m.amount==='number'?m.amount:1;from.node.connect(mg);mg.connect(param);modulations.push({from:m.from,to:m.to,param:m.param,amount:mg.gain.value,referenceId:m.referenceId});});
    (ir.automations||[]).forEach(applyAutomation);
    eventBindings=(ir.events||[]).map(function(e){return Object.assign({},e);});
    sidechains=(ir.sidechains||[]).map(function(s){return Object.assign({runtimeMode:'reference-only'},s);});
    bindRuntimeBus();
    ir.nodes.forEach(function(n){if(n.type==='master'&&nodes[n.id])nodes[n.id].node.connect(c.destination);});
    emit('ready',snapshot());
  }catch(e){ir.valid=false;ir.errors.push(String(e&&e.message||e));emit('error',{errors:ir.errors.slice()});}
  return snapshot();
}
function snapshot(){return {runtimeVersion:'0.16.3',contractVersion:'1.2',irVersion:ir.version||'1.5',valid:!!ir.valid,nodes:ir.nodes.map(function(n){var e=nodes[n.id];return{id:n.id,type:n.type,label:n.label,params:Object.assign({},n.params||{}),runtimeKind:e?e.runtimeKind:'not-built',controlActive:e?e.controlActive:true};}),connections:ir.connections.map(function(x){return{from:x.from,to:x.to,role:x.role,amount:x.amount,referenceId:x.referenceId};}),sends:sends.map(function(x){return{from:x.from,to:x.to,amount:x.amount,referenceId:x.referenceId};}),modulations:modulations.slice(),automations:automations.slice(),events:eventBindings.map(function(e){return Object.assign({},e);}),sidechains:sidechains.map(function(e){return Object.assign({},e);}),eventDeliveries:eventDeliveries.slice(),errors:(ir.errors||[]).slice(),warnings:(ir.warnings||[]).concat(runtimeWarnings).slice(),infos:(ir.infos||[]).slice(),contextState:ctx?ctx.state:'not-created'};}
function emit(type,data){document.dispatchEvent(new CustomEvent('sketsa:audio-tree:'+type,{detail:{source:'audio-tree',type:type,data:data}}));document.dispatchEvent(new CustomEvent('sketsa:runtime:event',{detail:{contractVersion:'1.2',source:'audio-tree',type:type,name:'audio-tree:'+type,data:data,__sketsaAudioTreeInternal:true}}));}
function resume(){try{build();return ctx&&ctx.resume?ctx.resume():Promise.resolve();}catch(e){return Promise.reject(e);}}
window.SketsaAudioTree={runtimeVersion:'0.16.3',contractVersion:'1.2',build:build,resume:resume,snapshot:snapshot,getContext:function(){return ctx;},emitRuntimeEvent:emitRuntimeEvent};
window.SketsaAudio={runtimeVersion:'0.16.3',contractVersion:'1.2',backend:'audio-tree',isTreePrimary:true,build:build,resume:resume,snapshot:snapshot,getContext:function(){return ctx;},emitRuntimeEvent:emitRuntimeEvent};
function updateControls(){
  var btn=document.getElementById('sketsa-audio-enable'),status=document.getElementById('sketsa-audio-runtime-status');
  if(btn&&!btn.__sketsaTreeBound){btn.__sketsaTreeBound=true;btn.addEventListener('click',function(){resume().then(updateControls).catch(function(e){if(status)status.textContent='Audio error: '+String(e&&e.message||e);});});}
  if(status){var st=snapshot();status.textContent=st.valid?'Audio Tree ready ('+st.contextState+')':'Audio Tree invalid';}
}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',function(){build();updateControls();},{once:true});else setTimeout(function(){build();updateControls();},0);
['pointerdown','keydown','touchstart','mousedown'].forEach(function(t){window.addEventListener(t,function(){resume().catch(function(){});},{capture:true,passive:true});});
})();
