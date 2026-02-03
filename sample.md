remote: Counting objects: 100% (55/55), done.
remote: Compressing objects: 100% (40/40), done.
remote: Total 55 (delta 13), reused 44 (delta 7), pack-reused 0 (from 0)  
Receiving objects: 100% (55/55), 83.49 KiB | 4.91 MiB/s, done.
Resolving deltas: 100% (13/13), done.
PS C:\Users\dell\.gemini\antigravity\scratch> cd 'C:\Users\dell\.gemini\antigravi
tigravity\scratch\Anti-ML-Training'
PS C:\Users\dell\.gemini\antigravity\scratch\Anti-ML-Training> & 'C:\Program File
am Files\Python311\python.exe' demo_prediction.py
============================================================
🤖 ML Node Selection - Demo
============================================================
📁 Model: C:\Users\dell\.gemini\antigravity\scratch\Anti-ML-Training\resou
rces\ml\model.pkl


🟢 Small Change (hotfix)
--------------------------------------------------
  📁 Files: 3, Lines: +50/-10
  📦 Deps: 0, Branch: hotfix/bug, Type: debug
C:\Users\dell\AppData\Roaming\Python\Python311\site-packages\sklearn\utils
\validation.py:2691: UserWarning: X does not have valid feature names, but
 StandardScaler was fitted with feature names
  warnings.warn(

  🔮 Prediction:
     💻 CPU:    22.4%
     💾 Memory: 1.0 GB
     ⏱️  Time:   1.4 min

  ✅ Selected: executor → T3a Small (2GB)

🟡 Medium Change (feature)
--------------------------------------------------
  📁 Files: 25, Lines: +700/-150
  📦 Deps: 0, Branch: feature/login, Type: debug
C:\Users\dell\AppData\Roaming\Python\Python311\site-packages\sklearn\utils
\validation.py:2691: UserWarning: X does not have valid feature names, but
 StandardScaler was fitted with feature names
  warnings.warn(

  🔮 Prediction:
     💻 CPU:    50.5%
     💾 Memory: 4.2 GB
     ⏱️  Time:   5.4 min

  ✅ Selected: build → T3a Large (8GB)

🟠 Large Change (refactor)
--------------------------------------------------
  📁 Files: 50, Lines: +1500/-400
  📦 Deps: 2, Branch: develop, Type: debug
C:\Users\dell\AppData\Roaming\Python\Python311\site-packages\sklearn\utils
\validation.py:2691: UserWarning: X does not have valid feature names, but
 StandardScaler was fitted with feature names
  warnings.warn(

  🔮 Prediction:
     💻 CPU:    72.1%
     💾 Memory: 8.1 GB
     ⏱️  Time:   10.1 min

  ✅ Selected: test → T3a X Large (16GB)

🔴 Release Build (main)
--------------------------------------------------
  📁 Files: 65, Lines: +1800/-500
  📦 Deps: 2, Branch: main, Type: release
C:\Users\dell\AppData\Roaming\Python\Python311\site-packages\sklearn\utils
\validation.py:2691: UserWarning: X does not have valid feature names, but
 StandardScaler was fitted with feature names
  warnings.warn(

  🔮 Prediction:
     💻 CPU:    76.4%
     💾 Memory: 9.6 GB
     ⏱️  Time:   11.5 min

  ✅ Selected: test → T3a X Large (16GB)

🟣 Massive Change (big feature)
--------------------------------------------------
  📁 Files: 80, Lines: +2200/-600
  📦 Deps: 3, Branch: feature/redesign, Type: debug
C:\Users\dell\AppData\Roaming\Python\Python311\site-packages\sklearn\utils
\validation.py:2691: UserWarning: X does not have valid feature names, but
 StandardScaler was fitted with feature names
  warnings.warn(

  🔮 Prediction:
     💻 CPU:    76.9%
     💾 Memory: 9.8 GB
     ⏱️  Time:   11.8 min

  ✅ Selected: test → T3a X Large (16GB)

============================================================
✅ Model is working correctly! Ready for POC demo.
============================================================
