// Valora Film — Interactive Script
// Segmented control switcher, FAQ accordions, and mobile navigation

document.addEventListener('DOMContentLoaded', () => {
  // Mobile drawer toggle
  const mobileMenuToggle = document.getElementById('mobileMenuToggle');
  const mobileDrawer = document.getElementById('mobileDrawer');

  if (mobileMenuToggle && mobileDrawer) {
    mobileMenuToggle.addEventListener('click', () => {
      mobileDrawer.classList.toggle('active');
    });

    document.querySelectorAll('.mobile-drawer-link').forEach(link => {
      link.addEventListener('click', () => {
        mobileDrawer.classList.remove('active');
      });
    });
  }

  // Smooth scroll for internal links
  document.querySelectorAll('a[href^="#"]').forEach(anchor => {
    anchor.addEventListener('click', function(e) {
      const targetId = this.getAttribute('href');
      if (targetId === '#') return;
      const targetElement = document.querySelector(targetId);
      if (targetElement) {
        e.preventDefault();
        targetElement.scrollIntoView({
          behavior: 'smooth',
          block: 'start'
        });
      }
    });
  });
});

// Segmented control tab switcher
function switchTab(tabKey) {
  // Update button active states
  const buttons = document.querySelectorAll('.segmented-option');
  buttons.forEach(btn => {
    btn.classList.remove('active');
    btn.setAttribute('aria-selected', 'false');
  });

  const activeBtn = document.getElementById('tab-' + tabKey);
  if (activeBtn) {
    activeBtn.classList.add('active');
    activeBtn.setAttribute('aria-selected', 'true');
  }

  // Update panels
  const panels = document.querySelectorAll('.tab-panel');
  panels.forEach(panel => panel.classList.remove('active'));

  const activePanel = document.getElementById('panel-' + tabKey);
  if (activePanel) {
    activePanel.classList.add('active');
  }
}

// FAQ Accordion toggle
function toggleFaq(rowElement) {
  const isOpen = rowElement.classList.contains('open');
  
  // Close any currently open FAQ item for clean accordion behavior
  document.querySelectorAll('.faq-row').forEach(row => {
    row.classList.remove('open');
  });

  if (!isOpen) {
    rowElement.classList.add('open');
  }
}
