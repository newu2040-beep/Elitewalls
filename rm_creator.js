const fs = require('fs');
const file = 'app/src/main/java/com/example/ui/screens/MainDashboard.kt';
let text = fs.readFileSync(file, 'utf8');

const startStr = '// ==================== CREATOR DASHBOARD ====================';
const endStr = '// ==================== SETTINGS SCREEN ====================';

const startIdx = text.indexOf(startStr);
const endIdx = text.indexOf(endStr);

if (startIdx !== -1 && endIdx !== -1) {
    const newText = text.substring(0, startIdx) + text.substring(endIdx);
    fs.writeFileSync(file, newText);
    console.log("Successfully removed Creator Dashboard");
} else {
    console.log("Could not find start or end tags");
}
