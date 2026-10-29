/* Local, dependency-free math engine for the PWA. */
(() => {
  'use strict';
  const FUNCTIONS = new Set(['sin','cos','tan','sec','csc','cot','asin','acos','atan','sinh','cosh','tanh','sqrt','ln','log','abs','exp']);
  const fmt = n => {
    if (!Number.isFinite(n)) return String(n);
    if (Math.abs(n) < 1e-12) return '0';
    return Number(n.toPrecision(10)).toString();
  };
  function tokenize(source) {
    const s = String(source).replace(/[×]/g,'*').replace(/[÷]/g,'/').replace(/[−]/g,'-').replace(/[π]/g,'pi');
    const tokens = []; let i = 0;
    while (i < s.length) {
      if (/\s/.test(s[i])) { i++; continue; }
      const rest = s.slice(i);
      const num = rest.match(/^(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][+-]?\d+)?/);
      if (num) { tokens.push({t:'num',v:Number(num[0])}); i += num[0].length; continue; }
      const id = rest.match(/^[a-zA-Z]+/);
      if (id) { tokens.push({t:'id',v:id[0].toLowerCase()}); i += id[0].length; continue; }
      if ('+-*/^()%'.includes(s[i])) { tokens.push({t:s[i],v:s[i]}); i++; continue; }
      throw Error('رمز غير مدعوم: ' + s[i]);
    }
    return tokens;
  }
  function parse(source) {
    if (!String(source).trim()) throw Error('اكتب الدالة أولًا');
    if (String(source).length > 300) throw Error('الدالة طويلة جدًا (الحد 300 حرف)');
    const ts = tokenize(source); let p = 0;
    const peek = () => ts[p];
    const eat = t => { if (peek() && peek().t === t) { p++; return true; } return false; };
    const primaryStart = () => !!peek() && (peek().t === 'num' || peek().t === 'id' || peek().t === '(');
    function primary() {
      const t = peek();
      if (!t) throw Error('تعبير غير مكتمل');
      if (eat('num')) return {k:'num',v:t.v};
      if (eat('(')) { const n = sum(); if (!eat(')')) throw Error('قوس إغلاق مفقود'); return n; }
      if (eat('id')) {
        if (t.v === 'x') return {k:'x'};
        if (t.v === 'pi') return {k:'num',v:Math.PI};
        if (t.v === 'e') return {k:'num',v:Math.E};
        if (!FUNCTIONS.has(t.v)) throw Error('اسم غير معروف: ' + t.v);
        if (!eat('(')) throw Error('اكتب قوسًا بعد الدالة ' + t.v);
        const a = sum(); if (!eat(')')) throw Error('قوس إغلاق مفقود في ' + t.v);
        return {k:'fn',name:t.v,a};
      }
      throw Error('تعبير غير مكتمل');
    }
    function postfix() {
      let n = primary();
      while (true) {
        if (eat('%')) n = {k:'bin',op:'/',a:n,b:{k:'num',v:100}};
        else break;
      }
      return n;
    }
    function unary() {
      if (eat('+')) return unary();
      if (eat('-')) return {k:'neg',a:unary()};
      return power();
    }
    function power() {
      const a = postfix();
      return eat('^') ? {k:'bin',op:'^',a,b:unary()} : a;
    }
    function product() {
      let n = unary();
      while (peek()) {
        if (eat('*')) n = {k:'bin',op:'*',a:n,b:unary()};
        else if (eat('/')) n = {k:'bin',op:'/',a:n,b:unary()};
        else if (primaryStart()) n = {k:'bin',op:'*',a:n,b:unary()};
        else break;
      }
      return n;
    }
    function sum() {
      let n = product();
      while (peek() && (peek().t === '+' || peek().t === '-')) {
        const op = ts[p++].t; n = {k:'bin',op,a:n,b:product()};
      }
      return n;
    }
    const tree = sum();
    if (p !== ts.length) throw Error('صيغة غير مكتملة قرب: ' + ts.slice(p).map(t=>t.v).join(''));
    return tree;
  }
  function evaluate(n, x) {
    switch (n.k) {
      case 'num': return n.v;
      case 'x': return x;
      case 'neg': return -evaluate(n.a,x);
      case 'bin': {
        const a=evaluate(n.a,x), b=evaluate(n.b,x);
        switch(n.op) {
          case '+': return a+b; case '-': return a-b; case '*': return a*b;
          case '/': return a/b; case '^': return Math.pow(a,b);
        }
        break;
      }
      case 'fn': {
        const a=evaluate(n.a,x);
        switch(n.name) {
          case 'sin': return Math.sin(a); case 'cos': return Math.cos(a); case 'tan': return Math.tan(a);
          case 'sec': return 1/Math.cos(a); case 'csc': return 1/Math.sin(a); case 'cot': return 1/Math.tan(a);
          case 'asin': return Math.asin(a); case 'acos': return Math.acos(a); case 'atan': return Math.atan(a);
          case 'sinh': return Math.sinh(a); case 'cosh': return Math.cosh(a); case 'tanh': return Math.tanh(a);
          case 'sqrt': return Math.sqrt(a); case 'ln': return Math.log(a); case 'log': return Math.log10(a);
          case 'abs': return Math.abs(a); case 'exp': return Math.exp(a);
        }
      }
    }
    return NaN;
  }
  const num = v => ({k:'num',v});
  const bin = (op,a,b) => ({k:'bin',op,a,b});
  const fn = (name,a) => ({k:'fn',name,a});
  function containsX(n) {
    if (n.k==='x') return true;
    if (n.k==='num') return false;
    if (n.k==='neg'||n.k==='fn') return containsX(n.a);
    return containsX(n.a)||containsX(n.b);
  }
  function derivative(n) {
    switch(n.k) {
      case 'num': return num(0);
      case 'x': return num(1);
      case 'neg': return {k:'neg',a:derivative(n.a)};
      case 'bin': {
        const a=n.a,b=n.b,da=derivative(a),db=derivative(b);
        if(n.op==='+') return bin('+',da,db);
        if(n.op==='-') return bin('-',da,db);
        if(n.op==='*') return bin('+',bin('*',da,b),bin('*',a,db));
        if(n.op==='/') return bin('/',bin('-',bin('*',da,b),bin('*',a,db)),bin('^',b,num(2)));
        if(n.op==='^') {
          if(!containsX(b)) return bin('*',bin('*',b,bin('^',a,bin('-',b,num(1)))),da);
          return bin('*',bin('^',a,b),bin('+',bin('*',db,fn('ln',a)),bin('*',b,bin('/',da,a))));
        }
        break;
      }
      case 'fn': {
        const a=n.a, da=derivative(a); let outer;
        switch(n.name) {
          case 'sin': outer=fn('cos',a); break;
          case 'cos': outer={k:'neg',a:fn('sin',a)}; break;
          case 'tan': outer=bin('/',num(1),bin('^',fn('cos',a),num(2))); break;
          case 'sec': outer=bin('*',fn('sec',a),fn('tan',a)); break;
          case 'csc': outer={k:'neg',a:bin('*',fn('csc',a),fn('cot',a))}; break;
          case 'cot': outer={k:'neg',a:bin('/',num(1),bin('^',fn('sin',a),num(2)))}; break;
          case 'ln': outer=bin('/',num(1),a); break;
          case 'log': outer=bin('/',num(1),bin('*',a,num(Math.LN10))); break;
          case 'exp': outer=fn('exp',a); break;
          case 'sqrt': outer=bin('/',num(1),bin('*',num(2),fn('sqrt',a))); break;
          case 'asin': outer=bin('/',num(1),fn('sqrt',bin('-',num(1),bin('^',a,num(2))))); break;
          case 'acos': outer={k:'neg',a:bin('/',num(1),fn('sqrt',bin('-',num(1),bin('^',a,num(2)))))}; break;
          case 'atan': outer=bin('/',num(1),bin('+',num(1),bin('^',a,num(2)))); break;
          case 'sinh': outer=fn('cosh',a); break;
          case 'cosh': outer=fn('sinh',a); break;
          case 'tanh': outer=bin('/',num(1),bin('^',fn('cosh',a),num(2))); break;
          case 'abs': outer=bin('/',a,fn('abs',a)); break;
          default: throw Error('الاشتقاق الرمزي غير مدعوم للدالة ' + n.name);
        }
        return bin('*',outer,da);
      }
    }
    throw Error('تعذر اشتقاق هذه الدالة');
  }
  function print(n,parent=0) {
    if(n.k==='num') return fmt(n.v);
    if(n.k==='x') return 'x';
    if(n.k==='neg') return '-'+print(n.a,4);
    if(n.k==='fn') return n.name+'('+print(n.a)+')';
    const prec = n.op==='+'||n.op==='-'?1:(n.op==='*'||n.op==='/')?2:3;
    const left=print(n.a,prec), right=print(n.b,prec+((n.op==='-'||n.op==='/')?1:0));
    const s=left+' '+n.op+' '+right;
    return prec<parent?'('+s+')':s;
  }
  function numericalLimit(tree, at) {
    if (at === Infinity || at === -Infinity) {
      const xs = at > 0 ? [1e2,1e3,1e4,1e5,1e6] : [-1e2,-1e3,-1e4,-1e5,-1e6];
      const ys = xs.map(x=>{try{return evaluate(tree,x)}catch{return NaN}}).filter(Number.isFinite);
      if(ys.length<3) return 'غير محددة عدديًا';
      const a=ys[ys.length-2],b=ys[ys.length-1];
      if(Math.abs(a-b)<1e-3*Math.max(1,Math.abs(b))) return '≈ '+fmt(b)+' (تقريب عددي)';
      if(Math.abs(b)>1e12) return b>0?'+∞':'−∞';
      return 'قد تكون غير منتهية أو غير موجودة';
    }
    const scales=[1e-2,1e-3,1e-4,1e-5,1e-6,1e-7];
    const side=dir=>scales.map(h=>{try{return evaluate(tree,at+dir*h)}catch{return NaN}}).filter(Number.isFinite);
    const left=side(-1),right=side(1);
    if(!left.length||!right.length) return 'النهاية غير محسومة عدديًا';
    const l=left[left.length-1],r=right[right.length-1];
    const tol=2e-3*Math.max(1,Math.abs(l),Math.abs(r));
    if(Math.abs(l-r)>tol) return 'النهاية الثنائية غير موجودة (اليسار '+fmt(l)+'، اليمين '+fmt(r)+')';
    return '≈ '+fmt((l+r)/2)+' (تقريب عددي)';
  }
  window.AQMath = {
    evaluate: (source, scope={}) => evaluate(parse(source), Number(scope.x ?? 0)),
    derivative: source => print(derivative(parse(source))),
    limit: (source, at) => numericalLimit(parse(source), at),
    parse, print
  };
  // Small compatibility facades keep the existing UI working without remote CDN files.
  window.math = { evaluate: (source, scope={}) => evaluate(parse(source), Number(scope.x ?? 0)) };
  const nerdamerCompat = function(source) {
    const textSource=String(source).trim();
    const match=textSource.match(/^limit\(([\s\S]+),\s*x\s*,\s*([^)]+)\)$/i);
    if(match) {
      const tree=parse(match[1]);
      let a=match[2].trim().replace(/∞/g,'Infinity').replace(/^\+?inf$/i,'Infinity').replace(/^-inf$/i,'-Infinity');
      const at=/^[+-]?Infinity$/i.test(a)?(a[0]==='-'?-Infinity:Infinity):Number(a);
      return {evaluate:()=>({text:()=>numericalLimit(tree,at)})};
    }
    const tree=parse(textSource);
    return {evaluate:()=>({text:()=>fmt(evaluate(tree,0))}),toString:()=>print(tree)};
  };
  nerdamerCompat.diff=(source, variable='x')=>{
    if(variable!=='x') throw Error('المتغير المدعوم حاليًا هو x');
    const tree=derivative(parse(source));
    return {toString:()=>print(tree),text:()=>print(tree)};
  };
  window.nerdamer=nerdamerCompat;
})();