/* ==========================================================================
   ATTEND HUB — Notion / Craft Student Hub Controller
   With Interactive Multi-Term Graph & Sidebar Overlay Pages
   ========================================================================== */

document.addEventListener('DOMContentLoaded', () => {
  const body = document.body;
  const toggleFrameBtn = document.getElementById('toggleFrameBtn');
  const toggleThemeBtn = document.getElementById('toggleThemeBtn');
  const themeLabel = document.getElementById('themeLabel');
  const iconSun = document.querySelector('.icon-sun');
  const iconMoon = document.querySelector('.icon-moon');
  const accentDots = document.querySelectorAll('.accent-dot');
  const presetSelect = document.getElementById('presetSelect');

  // Nav & Tabs
  const navItems = document.querySelectorAll('.nav-item');
  const tabPages = document.querySelectorAll('.tab-page');
  const overlayPages = document.querySelectorAll('.tab-page.overlay-page');

  // Sidebar
  const drawerTriggers = document.querySelectorAll('.side-menu-trigger');
  const sidebarDrawer = document.getElementById('sidebarDrawer');
  const drawerBackdrop = document.getElementById('drawerBackdrop');

  // Sidebar navigation items
  const drawerTimetableBtn = document.getElementById('drawerTimetableBtn');
  const drawerHolidayBtn = document.getElementById('drawerHolidayBtn');
  const drawerSettingsBtn = document.getElementById('drawerSettingsBtn');
  const drawerPrivacyBtn = document.getElementById('drawerPrivacyBtn');
  const backSidebarBtns = document.querySelectorAll('.back-sidebar-page');

  // Bottom Sheet & FAB
  const mainFab = document.getElementById('mainFab');
  const quickActionSheet = document.getElementById('quickActionSheet');
  const bottomSheetBackdrop = document.getElementById('bottomSheetBackdrop');

  // Modals & Toast
  const modalOverlay = document.getElementById('modalOverlay');
  const modalTitle = document.getElementById('modalTitle');
  const modalBody = document.getElementById('modalBody');
  const modalCloseBtn = document.getElementById('modalCloseBtn');
  const modalCancelBtn = document.getElementById('modalCancelBtn');
  const modalSubmitBtn = document.getElementById('modalSubmitBtn');
  const toast = document.getElementById('toastNotification');
  const toastMessage = document.getElementById('toastMessage');

  // Subject Detail Page
  const subjectItems = document.querySelectorAll('.subject-item');
  const pageSubjectDetail = document.getElementById('pageSubjectDetail');
  const backFromSubjectBtn = document.getElementById('backFromSubjectBtn');
  const detailSubjectTitle = document.getElementById('detailSubjectTitle');

  // Marks Multi-Term Compare Mode Elements
  const modeTableBtn = document.getElementById('modeTableBtn');
  const modeCompareBtn = document.getElementById('modeCompareBtn');
  const marksTableViewBlock = document.getElementById('marksTableViewBlock');
  const marksCompareViewBlock = document.getElementById('marksCompareViewBlock');
  const compareSubjectSelect = document.getElementById('compareSubjectSelect');

  // Course multi-term score datasets for graph comparison
  const compareDatasets = {
    dms: {
      name: 'Database Management Systems',
      growth: '+50% Growth',
      t1: { raw: '10/24', pct: 41.7, y: 110 },
      t2: { raw: '16/24', pct: 66.7, y: 62 },
      t3: { raw: '22/24', pct: 91.7, y: 30 },
      curvePath: 'M 60,110 L 170,62 L 280,30',
      fillPoints: '60,110 170,62 280,30 280,130 60,130'
    },
    cn: {
      name: 'Computer Networks',
      growth: '+16.7% Growth',
      t1: { raw: '20/24', pct: 83.3, y: 45 },
      t2: { raw: '21/24', pct: 87.5, y: 38 },
      t3: { raw: '24/24', pct: 100.0, y: 20 },
      curvePath: 'M 60,45 L 170,38 L 280,20',
      fillPoints: '60,45 170,38 280,20 280,130 60,130'
    },
    se: {
      name: 'Software Engineering',
      growth: '+25.0% Growth',
      t1: { raw: '18/24', pct: 75.0, y: 55 },
      t2: { raw: '20/24', pct: 83.3, y: 45 },
      t3: { raw: '24/24', pct: 100.0, y: 20 },
      curvePath: 'M 60,55 L 170,45 L 280,20',
      fillPoints: '60,55 170,45 280,20 280,130 60,130'
    },
    erp: {
      name: 'Enterprise Resource Planning',
      growth: '+12.5% Growth',
      t1: { raw: '14/24', pct: 58.3, y: 80 },
      t2: { raw: '15/24', pct: 62.5, y: 72 },
      t3: { raw: '17/24', pct: 70.8, y: 58 },
      curvePath: 'M 60,80 L 170,72 L 280,58',
      fillPoints: '60,80 170,72 280,58 280,130 60,130'
    }
  };

  /* --------------------------------------------------------------------------
     1. Web Toolbar Controls
     -------------------------------------------------------------------------- */
  toggleFrameBtn.addEventListener('click', () => {
    body.classList.toggle('device-frame-mode');
    toggleFrameBtn.classList.toggle('active', body.classList.contains('device-frame-mode'));
  });

  toggleThemeBtn.addEventListener('click', () => {
    const isDark = body.classList.toggle('theme-dark');
    body.classList.toggle('theme-light', !isDark);
    themeLabel.textContent = isDark ? 'Dark' : 'Light';
    iconSun.classList.toggle('hidden', isDark);
    iconMoon.classList.toggle('hidden', !isDark);
  });

  accentDots.forEach(dot => {
    dot.addEventListener('click', () => {
      accentDots.forEach(d => d.classList.remove('active'));
      dot.classList.add('active');
      const accent = dot.dataset.accent;
      body.className = body.className.replace(/accent-\w+/g, `accent-${accent}`);
    });
  });

  const demoStatePills = document.querySelectorAll('.demo-state-pill');

  presetSelect.addEventListener('change', (e) => applyPreset(e.target.value));

  demoStatePills.forEach(pill => {
    pill.addEventListener('click', () => {
      const state = pill.dataset.state;
      if (presetSelect) presetSelect.value = state;
      applyPreset(state);
    });
  });

  function applyPreset(preset) {
    demoStatePills.forEach(p => p.classList.toggle('active', p.dataset.state === preset));

    // Home Hero Card DOM
    const homeGaugePct = document.getElementById('homeGaugePct');
    const homeRingArc = document.getElementById('homeRingArc');
    const homeStatusChip = document.getElementById('homeStatusChip');
    const homeStatusChipLabel = document.getElementById('homeStatusChipLabel');
    const homeStatusHeadline = document.getElementById('homeStatusHeadline');
    const homeStatusSub = document.getElementById('homeStatusSub');

    // Attendance Hero Card DOM
    const attSemSubtitle = document.getElementById('attSemSubtitle');
    const attGaugePct = document.getElementById('attGaugePct');
    const attRingArc = document.getElementById('attRingArc');
    const attStatusChip = document.getElementById('attStatusChip');
    const attStatusChipLabel = document.getElementById('attStatusChipLabel');
    const attStreakText = document.getElementById('attStreakText');
    const attStatusHeadline = document.getElementById('attStatusHeadline');
    const attTargetNote = document.getElementById('attTargetNote');
    const attStatAttended = document.getElementById('attStatAttended');
    const attStatConducted = document.getElementById('attStatConducted');
    const attStatMissed = document.getElementById('attStatMissed');
    const attStatCancelled = document.getElementById('attStatCancelled');

    const todayClassesList = document.getElementById('todayClassesList');
    const todayClassesEmpty = document.getElementById('todayClassesEmpty');

    const totalCircumference = 301.6;
    const calcOffset = (pct) => (totalCircumference * (1 - Math.min(100, Math.max(0, pct)) / 100)).toFixed(1);

    if (preset === 'recoverable') {
      // 32 / 50 attended = 64%. 38 classes left in semester.
      // Needs 22 classes to reach 75%. Max possible if all 38 left attended = 80%.
      const curPct = 64;
      homeGaugePct.textContent = `${curPct}%`;
      homeGaugePct.classList.remove('text-danger');
      attGaugePct.textContent = `${curPct}%`;
      attGaugePct.classList.remove('text-danger');
      if (attSemSubtitle) attSemSubtitle.textContent = `Semester 5 · Jul – Nov · ${curPct}% overall`;

      if (homeRingArc) { homeRingArc.style.strokeDashoffset = calcOffset(curPct); homeRingArc.setAttribute('class', 'ring-arc warning'); }
      if (attRingArc) { attRingArc.style.strokeDashoffset = calcOffset(curPct); attRingArc.setAttribute('class', 'ring-arc warning'); }

      if (homeStatusChip) homeStatusChip.className = 'status-chip chip-warning';
      if (homeStatusChipLabel) homeStatusChipLabel.textContent = 'Action Needed';
      if (homeStatusHeadline) {
        homeStatusHeadline.classList.remove('text-danger');
        homeStatusHeadline.innerHTML = 'Attend next <strong>22 classes</strong>';
      }
      if (homeStatusSub) {
        homeStatusSub.innerHTML = '<strong>38 classes left</strong> in semester · Max possible: <strong>80%</strong>';
      }

      if (attStatusChip) attStatusChip.className = 'status-chip chip-warning';
      if (attStatusChipLabel) attStatusChipLabel.textContent = 'Action Needed';
      if (attStreakText) attStreakText.textContent = '4-Day Streak · Best: 9 Days';
      if (attStatusHeadline) {
        attStatusHeadline.classList.remove('text-danger');
        attStatusHeadline.innerHTML = 'Attend next <strong>22 classes</strong>';
      }
      if (attTargetNote) {
        attTargetNote.innerHTML = '<strong>38 classes left</strong> in semester · Max possible: <strong>80%</strong>';
      }
      if (attStatAttended) attStatAttended.textContent = '32';
      if (attStatConducted) attStatConducted.textContent = '50';
      if (attStatMissed) attStatMissed.textContent = '18';
      if (attStatCancelled) attStatCancelled.textContent = '3';

      todayClassesList.classList.remove('hidden');
      todayClassesEmpty.classList.add('hidden');
      showToast('Recoverable: Attend next 22 (38 left · Max 80%)');

    } else if (preset === 'unreachable') {
      // 27 / 60 attended = 45%. Only 18 classes left in semester.
      // Even if all 18 remaining classes are attended: (27 + 18) / (60 + 18) = 45 / 78 = 58% Max (< 75%).
      const curPct = 45;
      homeGaugePct.textContent = `${curPct}%`;
      homeGaugePct.classList.add('text-danger');
      attGaugePct.textContent = `${curPct}%`;
      attGaugePct.classList.add('text-danger');
      if (attSemSubtitle) attSemSubtitle.textContent = `Semester 5 · Jul – Nov · ${curPct}% overall`;

      if (homeRingArc) { homeRingArc.style.strokeDashoffset = calcOffset(curPct); homeRingArc.setAttribute('class', 'ring-arc danger'); }
      if (attRingArc) { attRingArc.style.strokeDashoffset = calcOffset(curPct); attRingArc.setAttribute('class', 'ring-arc danger'); }

      if (homeStatusChip) homeStatusChip.className = 'status-chip chip-risk';
      if (homeStatusChipLabel) homeStatusChipLabel.textContent = 'Target Unreachable';
      if (homeStatusHeadline) {
        homeStatusHeadline.classList.add('text-danger');
        homeStatusHeadline.innerHTML = 'Attend all <strong>18 classes left</strong>';
      }
      if (homeStatusSub) {
        homeStatusSub.innerHTML = 'Cannot reach 75% target · Max possible is <strong style="color: var(--danger);">58%</strong>';
      }

      if (attStatusChip) attStatusChip.className = 'status-chip chip-risk';
      if (attStatusChipLabel) attStatusChipLabel.textContent = 'Target Unreachable';
      if (attStreakText) attStreakText.textContent = 'Best Streak: 5 Days';
      if (attStatusHeadline) {
        attStatusHeadline.classList.add('text-danger');
        attStatusHeadline.innerHTML = 'Attend all <strong>18 classes left</strong>';
      }
      if (attTargetNote) {
        attTargetNote.innerHTML = 'Cannot reach 75% target · Max possible is <strong style="color: var(--danger);">58%</strong>';
      }
      if (attStatAttended) attStatAttended.textContent = '27';
      if (attStatConducted) attStatConducted.textContent = '60';
      if (attStatMissed) attStatMissed.textContent = '33';
      if (attStatCancelled) attStatCancelled.textContent = '2';

      todayClassesList.classList.remove('hidden');
      todayClassesEmpty.classList.add('hidden');
      showToast('Unreachable: Attend all 18 left (Max possible 58%)');

    } else if (preset === 'empty') {
      homeGaugePct.textContent = '0%';
      homeGaugePct.classList.remove('text-danger');
      attGaugePct.textContent = '0%';
      attGaugePct.classList.remove('text-danger');
      if (attSemSubtitle) attSemSubtitle.textContent = 'Semester 5 · Jul – Nov · Fresh Term';

      if (homeRingArc) { homeRingArc.style.strokeDashoffset = totalCircumference; homeRingArc.setAttribute('class', 'ring-arc'); }
      if (attRingArc) { attRingArc.style.strokeDashoffset = totalCircumference; attRingArc.setAttribute('class', 'ring-arc'); }

      if (homeStatusChip) homeStatusChip.className = 'status-chip chip-secondary';
      if (homeStatusChipLabel) homeStatusChipLabel.textContent = 'Fresh Term';
      if (homeStatusHeadline) {
        homeStatusHeadline.classList.remove('text-danger');
        homeStatusHeadline.innerHTML = 'No classes conducted yet';
      }
      if (homeStatusSub) {
        homeStatusSub.innerHTML = '<strong>120 classes left</strong> in semester · Target: <strong>75%</strong>';
      }

      if (attStatusChip) attStatusChip.className = 'status-chip chip-secondary';
      if (attStatusChipLabel) attStatusChipLabel.textContent = 'Fresh Term';
      if (attStreakText) attStreakText.textContent = '0-Day Streak';
      if (attStatusHeadline) {
        attStatusHeadline.classList.remove('text-danger');
        attStatusHeadline.innerHTML = 'No classes conducted yet';
      }
      if (attTargetNote) {
        attTargetNote.innerHTML = '<strong>120 classes left</strong> in semester · Target: <strong>75%</strong>';
      }
      if (attStatAttended) attStatAttended.textContent = '0';
      if (attStatConducted) attStatConducted.textContent = '0';
      if (attStatMissed) attStatMissed.textContent = '0';
      if (attStatCancelled) attStatCancelled.textContent = '0';

      todayClassesList.classList.add('hidden');
      todayClassesEmpty.classList.remove('hidden');
      showToast('Fresh Term: 120 classes scheduled');

    } else {
      // Safe Margin: 48 / 58 attended = 82%. 38 classes left in semester. Max = 90%.
      const curPct = 82;
      homeGaugePct.textContent = `${curPct}%`;
      homeGaugePct.classList.remove('text-danger');
      attGaugePct.textContent = `${curPct}%`;
      attGaugePct.classList.remove('text-danger');
      if (attSemSubtitle) attSemSubtitle.textContent = `Semester 5 · Jul – Nov · ${curPct}% overall`;

      if (homeRingArc) { homeRingArc.style.strokeDashoffset = calcOffset(curPct); homeRingArc.setAttribute('class', 'ring-arc safe'); }
      if (attRingArc) { attRingArc.style.strokeDashoffset = calcOffset(curPct); attRingArc.setAttribute('class', 'ring-arc safe'); }

      if (homeStatusChip) homeStatusChip.className = 'status-chip chip-safe';
      if (homeStatusChipLabel) homeStatusChipLabel.textContent = 'Safe Margin';
      if (homeStatusHeadline) {
        homeStatusHeadline.classList.remove('text-danger');
        homeStatusHeadline.innerHTML = 'Can safely skip <strong>5 classes</strong>';
      }
      if (homeStatusSub) {
        homeStatusSub.innerHTML = '<strong>38 classes left</strong> in semester · Max possible: <strong>90%</strong>';
      }

      if (attStatusChip) attStatusChip.className = 'status-chip chip-safe';
      if (attStatusChipLabel) attStatusChipLabel.textContent = 'Safe Margin';
      if (attStreakText) attStreakText.textContent = '6-Day Streak · Best: 9 Days';
      if (attStatusHeadline) {
        attStatusHeadline.classList.remove('text-danger');
        attStatusHeadline.innerHTML = 'Can safely skip <strong>5 classes</strong>';
      }
      if (attTargetNote) {
        attTargetNote.innerHTML = '<strong>38 classes left</strong> in semester · Max possible: <strong>90%</strong>';
      }
      if (attStatAttended) attStatAttended.textContent = '48';
      if (attStatConducted) attStatConducted.textContent = '58';
      if (attStatMissed) attStatMissed.textContent = '10';
      if (attStatCancelled) attStatCancelled.textContent = '3';

      todayClassesList.classList.remove('hidden');
      todayClassesEmpty.classList.add('hidden');
      showToast('Safe Margin: Can skip 5 (38 left · Max 90%)');
    }
  }

  let toastTimer;

  // Initialize default preset on load
  applyPreset('recoverable');

  /* --------------------------------------------------------------------------
     2. Tab Navigation
     -------------------------------------------------------------------------- */
  navItems.forEach(item => {
    item.addEventListener('click', () => {
      const targetId = item.dataset.target;
      navItems.forEach(n => n.classList.remove('active'));
      item.classList.add('active');

      tabPages.forEach(page => {
        page.classList.remove('active');
        if (page.id === targetId) page.classList.add('active');
      });

      closeAllOverlayPages();
    });
  });

  document.getElementById('seeAllTasksBtn')?.addEventListener('click', () => switchToTab('pageTasks'));
  document.getElementById('homeHeroCard')?.addEventListener('click', () => switchToTab('pageAttendance'));
  document.getElementById('homeViewAnalyticsLink')?.addEventListener('click', (e) => {
    e.stopPropagation();
    switchToTab('pageAttendance');
  });
  document.getElementById('addSubjectNavBtn')?.addEventListener('click', () => openModal('Add Subject'));
  document.getElementById('addMarksNavBtn')?.addEventListener('click', () => openModal('Add Marks'));

  function switchToTab(tabId) {
    navItems.forEach(n => n.classList.toggle('active', n.dataset.target === tabId));
    tabPages.forEach(p => p.classList.toggle('active', p.id === tabId));
    closeAllOverlayPages();
  }

  function closeAllOverlayPages() {
    overlayPages.forEach(page => page.classList.remove('active'));
  }

  /* --------------------------------------------------------------------------
     3. Drawer / Sidebar & Overlay Page Openers
     -------------------------------------------------------------------------- */
  drawerTriggers.forEach(t => t.addEventListener('click', openDrawer));
  drawerBackdrop.addEventListener('click', closeDrawer);

  function openDrawer() {
    sidebarDrawer.classList.add('active');
    drawerBackdrop.classList.add('active');
  }

  function closeDrawer() {
    sidebarDrawer.classList.remove('active');
    drawerBackdrop.classList.remove('active');
  }

  // Open sidebar overlay pages
  const drawerHeaderProfile = document.getElementById('drawerHeaderProfile');
  const drawerProfileBtn = document.getElementById('drawerProfileBtn');

  drawerHeaderProfile?.addEventListener('click', () => {
    closeDrawer();
    openOverlayPage('pageProfile');
  });

  drawerProfileBtn?.addEventListener('click', (e) => {
    e.stopPropagation();
    closeDrawer();
    openOverlayPage('pageProfile');
  });

  drawerTimetableBtn?.addEventListener('click', () => {
    closeDrawer();
    openOverlayPage('pageTimetable');
  });

  drawerHolidayBtn?.addEventListener('click', () => {
    closeDrawer();
    openOverlayPage('pageHolidays');
  });

  drawerSettingsBtn?.addEventListener('click', () => {
    closeDrawer();
    openOverlayPage('pageSettings');
  });

  drawerPrivacyBtn?.addEventListener('click', () => {
    closeDrawer();
    openOverlayPage('pagePrivacy');
  });

  // Profile Save Action
  const saveProfileBtn = document.getElementById('saveProfileBtn');
  const profInputName = document.getElementById('profInputName');
  const profInputCollege = document.getElementById('profInputCollege');
  const profInputBranch = document.getElementById('profInputBranch');
  const profSelectSem = document.getElementById('profSelectSem');
  const profInputRoll = document.getElementById('profInputRoll');

  saveProfileBtn?.addEventListener('click', () => {
    const name = profInputName ? profInputName.value.trim() : 'Rashid Ekbal';
    const branch = profInputBranch ? profInputBranch.value.trim() : 'Computer Science & Engineering';
    const sem = profSelectSem ? profSelectSem.value : 'Sem 5';
    const roll = profInputRoll ? profInputRoll.value.trim() : '2023CSE089';

    // Calculate initials
    const initials = name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2) || 'RE';

    // Update UI Elements across app
    document.getElementById('profileHeroName').textContent = name;
    document.getElementById('profileHeroSub').textContent = `${branch} · ${sem}`;
    document.getElementById('profSemTag').textContent = sem;
    document.getElementById('profRollTag').textContent = `Roll #${roll}`;
    document.getElementById('profileAvatarBig').textContent = initials;

    document.getElementById('drawerUserName').textContent = name;
    document.getElementById('drawerUserSub').textContent = `${branch} · ${sem}`;
    document.getElementById('drawerAvatar').textContent = initials;

    showToast('Student profile updated successfully!');
    closeAllOverlayPages();
  });

  // Export Data & Reset Data Buttons
  document.getElementById('exportDataBtn')?.addEventListener('click', () => {
    showToast('Academic data exported as attend_backup.json');
  });

  document.getElementById('resetTermBtn')?.addEventListener('click', () => {
    if (confirm('Are you sure you want to reset current term attendance data?')) {
      applyPreset('empty');
      showToast('Term data reset to 0%');
      closeAllOverlayPages();
    }
  });

  backSidebarBtns.forEach(btn => {
    btn.addEventListener('click', closeAllOverlayPages);
  });

  function openOverlayPage(pageId) {
    closeAllOverlayPages();
    const page = document.getElementById(pageId);
    if (page) page.classList.add('active');
  }

  // Settings threshold slider
  const thresholdSlider = document.getElementById('thresholdSlider');
  const thresholdValDisplay = document.getElementById('thresholdValDisplay');
  thresholdSlider?.addEventListener('input', (e) => {
    if (thresholdValDisplay) thresholdValDisplay.textContent = `${e.target.value}%`;
  });

  /* --------------------------------------------------------------------------
     4. Marks Multi-Term Compare Mode & Dynamic Graph Updates
     -------------------------------------------------------------------------- */
  modeTableBtn?.addEventListener('click', () => {
    modeTableBtn.classList.add('active');
    modeCompareBtn.classList.remove('active');
    marksTableViewBlock.classList.remove('hidden');
    marksCompareViewBlock.classList.add('hidden');
  });

  modeCompareBtn?.addEventListener('click', () => {
    modeCompareBtn.classList.add('active');
    modeTableBtn.classList.remove('active');
    marksCompareViewBlock.classList.remove('hidden');
    marksTableViewBlock.classList.add('hidden');
    updateCompareGraph(compareSubjectSelect.value);
  });

  compareSubjectSelect?.addEventListener('change', (e) => {
    updateCompareGraph(e.target.value);
  });

  function updateCompareGraph(subjectKey) {
    const data = compareDatasets[subjectKey] || compareDatasets.dms;

    document.getElementById('graphCourseName').textContent = data.name;
    document.getElementById('graphImprovementChip').textContent = data.growth;

    const graphCurvePath = document.getElementById('graphCurvePath');
    const graphFillArea = document.getElementById('graphFillArea');

    if (graphCurvePath) graphCurvePath.setAttribute('d', data.curvePath);
    if (graphFillArea) graphFillArea.setAttribute('points', data.fillPoints);

    // Update Dots & Labels
    const dot1 = document.getElementById('dotTerm1');
    const dot2 = document.getElementById('dotTerm2');
    const dot3 = document.getElementById('dotTerm3');
    if (dot1) dot1.setAttribute('cy', data.t1.y);
    if (dot2) dot2.setAttribute('cy', data.t2.y);
    if (dot3) dot3.setAttribute('cy', data.t3.y);

    document.getElementById('valTerm1').textContent = `${data.t1.raw} (${data.t1.pct}%)`;
    document.getElementById('valTerm2').textContent = `${data.t2.raw} (${data.t2.pct}%)`;
    document.getElementById('valTerm3').textContent = `${data.t3.raw} (${data.t3.pct}%)`;

    // Update Term Bars
    document.getElementById('barScoreTerm1').textContent = data.t1.raw;
    document.getElementById('barScoreTerm2').textContent = data.t2.raw;
    document.getElementById('barScoreTerm3').textContent = data.t3.raw;

    document.getElementById('barFillTerm1').style.width = `${data.t1.pct}%`;
    document.getElementById('barFillTerm2').style.width = `${data.t2.pct}%`;
    document.getElementById('barFillTerm3').style.width = `${data.t3.pct}%`;

    showToast(`Loaded 3-Term Graph: ${data.name}`);
  }

  /* --------------------------------------------------------------------------
     5. Bottom Sheet & FAB
     -------------------------------------------------------------------------- */
  mainFab.addEventListener('click', openBottomSheet);
  bottomSheetBackdrop.addEventListener('click', closeBottomSheet);

  function openBottomSheet() {
    const isOpen = quickActionSheet.classList.toggle('active');
    bottomSheetBackdrop.classList.toggle('active', isOpen);
    mainFab.classList.toggle('sheet-open', isOpen);
  }

  function closeBottomSheet() {
    quickActionSheet.classList.remove('active');
    bottomSheetBackdrop.classList.remove('active');
    mainFab.classList.remove('sheet-open');
  }

  document.getElementById('sheetAddAssignment')?.addEventListener('click', () => { closeBottomSheet(); openModal('Add Assignment'); });
  document.getElementById('sheetAddSubject')?.addEventListener('click', () => { closeBottomSheet(); openModal('Add Subject'); });
  document.getElementById('sheetAddMarks')?.addEventListener('click', () => { closeBottomSheet(); openModal('Add Marks'); });
  document.getElementById('sheetAddHoliday')?.addEventListener('click', () => { closeBottomSheet(); openModal('Add Holiday'); });
  document.getElementById('taskHeaderAddBtn')?.addEventListener('click', () => openModal('Add Assignment'));
  document.getElementById('addHolidayHeaderBtn')?.addEventListener('click', () => openModal('Add Holiday'));

  /* --------------------------------------------------------------------------
     6. Modals & Forms
     -------------------------------------------------------------------------- */
  modalCloseBtn.addEventListener('click', closeModal);
  modalCancelBtn.addEventListener('click', closeModal);
  modalOverlay.addEventListener('click', (e) => { if (e.target === modalOverlay) closeModal(); });

  modalSubmitBtn.addEventListener('click', () => {
    showToast(`${modalTitle.textContent} saved successfully!`);
    closeModal();
  });

  function openModal(type) {
    modalTitle.textContent = type;
    modalBody.innerHTML = generateFormHtml(type);
    modalOverlay.classList.add('active');
  }

  function closeModal() {
    modalOverlay.classList.remove('active');
  }

  function generateFormHtml(type) {
    if (type === 'Add Assignment') {
      return `
        <div class="form-group">
          <label>Assignment Title</label>
          <input type="text" class="form-input" placeholder="e.g. OS Assignment 3" value="Design Patterns Analysis" />
        </div>
        <div class="form-group">
          <label>Subject</label>
          <input type="text" class="form-input" value="Software Engineering" />
        </div>
        <div class="form-group">
          <label>Due Date</label>
          <input type="date" class="form-input" value="2026-08-28" />
        </div>
        <div class="form-group">
          <label>Reminder</label>
          <input type="text" class="form-input" value="1 day before" />
        </div>
      `;
    } else if (type === 'Add Subject') {
      return `
        <div class="form-group">
          <label>Subject Name</label>
          <input type="text" class="form-input" placeholder="e.g. Compiler Design" />
        </div>
        <div class="form-group">
          <label>Target Criteria (%)</label>
          <input type="number" class="form-input" value="75" />
        </div>
        <div class="form-group">
          <label>Weekly Classes Count</label>
          <input type="number" class="form-input" value="4" />
        </div>
      `;
    } else if (type === 'Add Marks') {
      return `
        <div class="form-group">
          <label>Subject</label>
          <input type="text" class="form-input" value="Database Management Systems" />
        </div>
        <div class="form-group">
          <label>Exam Type</label>
          <input type="text" class="form-input" value="Mid Sem 1" />
        </div>
        <div class="form-group">
          <label>Scored Marks</label>
          <input type="number" class="form-input" value="18" />
        </div>
        <div class="form-group">
          <label>Total Marks</label>
          <input type="number" class="form-input" value="24" />
        </div>
      `;
    } else {
      return `
        <div class="form-group">
          <label>Holiday Title</label>
          <input type="text" class="form-input" value="Independence Day" />
        </div>
        <div class="form-group">
          <label>Date</label>
          <input type="date" class="form-input" value="2026-08-15" />
        </div>
      `;
    }
  }

  /* --------------------------------------------------------------------------
     7. Class Actions & Toggles
     -------------------------------------------------------------------------- */
  const classItems = document.querySelectorAll('.class-card-item');
  classItems.forEach(item => {
    const pBtn = item.querySelector('.p-btn');
    const aBtn = item.querySelector('.a-btn');
    const cBtn = item.querySelector('.c-btn');
    const className = item.querySelector('.class-name')?.textContent || 'Class';

    if (pBtn && aBtn && cBtn) {
      pBtn.addEventListener('click', () => {
        clearActions(pBtn, aBtn, cBtn);
        pBtn.classList.add('active-p');
        showToast(`Marked Present: ${className}`);
      });
      aBtn.addEventListener('click', () => {
        clearActions(pBtn, aBtn, cBtn);
        aBtn.classList.add('active-a');
        showToast(`Marked Absent: ${className}`);
      });
      cBtn.addEventListener('click', () => {
        clearActions(pBtn, aBtn, cBtn);
        cBtn.classList.add('active-c');
        showToast(`Marked Cancelled: ${className}`);
      });
    }
  });

  function clearActions(p, a, c) {
    p.classList.remove('active-p');
    a.classList.remove('active-a');
    c.classList.remove('active-c');
  }

  document.getElementById('markAllPresentBtn')?.addEventListener('click', () => {
    classItems.forEach(item => {
      const pBtn = item.querySelector('.p-btn');
      const aBtn = item.querySelector('.a-btn');
      const cBtn = item.querySelector('.c-btn');
      if (pBtn && aBtn && cBtn) {
        clearActions(pBtn, aBtn, cBtn);
        pBtn.classList.add('active-p');
      }
    });
    showToast('All classes marked Present!');
  });

  const submitPills = document.querySelectorAll('.btn-submit-pill');
  submitPills.forEach(btn => {
    btn.addEventListener('click', () => {
      const isSubmitted = btn.classList.toggle('active');
      btn.textContent = isSubmitted ? '✓ Submitted' : '✓ Submit';
      showToast(isSubmitted ? 'Assignment marked Submitted' : 'Assignment marked Pending');
    });
  });

  /* --------------------------------------------------------------------------
     8. Subject Detail Drilldown
     -------------------------------------------------------------------------- */
  subjectItems.forEach(item => {
    item.addEventListener('click', () => {
      const name = item.querySelector('.subject-name').textContent;
      detailSubjectTitle.textContent = name;
      document.getElementById('detailSubjectSub').textContent = `Theory Subject · 75% Criteria`;
      openOverlayPage('pageSubjectDetail');
    });
  });

  backFromSubjectBtn?.addEventListener('click', closeAllOverlayPages);

  /* --------------------------------------------------------------------------
     9. Toast Helper
     -------------------------------------------------------------------------- */
  function showToast(msg) {
    toastMessage.textContent = msg;
    toast.classList.add('active');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => {
      toast.classList.remove('active');
    }, 2400);
  }
});
