// Quick test for whisper wasm bindings in Node
try {
  const fs = require('fs');
  const path = require('path');
  console.log('Testing whisper.cpp wasm loading in Node...');
  // Check if file exists
  const wasmJsPath = path.join(__dirname, 'dashboard', 'public', 'whisper', 'whisper.cpp.wasm.js');
  console.log('wasmJsPath exists:', fs.existsSync(wasmJsPath));
} catch (e) {
  console.error('Error:', e);
}
