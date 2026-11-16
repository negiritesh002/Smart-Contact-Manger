const toggleSidebar = () => {
    const sidebar = document.querySelector('.sidebar');
    if (!sidebar) return;
    const mobile = window.matchMedia('(max-width: 767px)').matches;
    document.body.classList.toggle(mobile ? 'sidebar-open' : 'sidebar-hidden');
    syncSidebar();
};
const syncSidebar = () => {
    const sidebar = document.querySelector('.sidebar');
    if (!sidebar) return;
    const mobile = window.matchMedia('(max-width: 767px)').matches;
    const expanded = mobile ? document.body.classList.contains('sidebar-open') : !document.body.classList.contains('sidebar-hidden');
    document.querySelector('.sidebar-toggle')?.setAttribute('aria-expanded', String(expanded));
    sidebar.inert = !expanded;
};
document.addEventListener('DOMContentLoaded', () => {
    syncSidebar();
    window.matchMedia('(max-width: 767px)').addEventListener('change', syncSidebar);
    const path = window.location.pathname;
    const navId = path === '/user/index' ? 'home-link' : path.includes('/show-contacts/') || path.endsWith('/contact') ? 'contacts-link' : path.includes('add-contact') || path.includes('update-contact') ? 'add-link' : path.endsWith('/profile') ? 'profile-link' : path.endsWith('/settings') ? 'settings-link' : null;
    document.querySelectorAll('.sidebar .item').forEach(link => {
        const active = link.id === navId;
        link.classList.toggle('active', active);
        if (active) link.setAttribute('aria-current', 'page');
    });
    document.querySelectorAll('.navbar-nav .nav-link').forEach(link => {
        if (link.getAttribute('href') === path) {
            link.parentElement.classList.add('active');
            link.setAttribute('aria-current', 'page');
        }
    });
    document.querySelectorAll('[data-contact-search]').forEach(input => {
        const results = document.getElementById(input.getAttribute('aria-controls'));
        let timer, controller, requestId = 0;
        const close = () => { results.style.display = 'none'; input.setAttribute('aria-expanded', 'false'); };
        const message = text => { results.replaceChildren(); const p = document.createElement('p'); p.textContent = text; results.append(p); results.style.display = 'block'; input.setAttribute('aria-expanded', 'true'); };
        input.addEventListener('input', () => {
            clearTimeout(timer);
            controller?.abort();
            const id = ++requestId;
            const query = input.value.trim();
            if (!query) { close(); return; }
            timer = setTimeout(async () => {
                controller = new AbortController();
                message('Searching your contacts...');
                try {
                    const response = await fetch('/search/' + encodeURIComponent(query), { signal: controller.signal, headers: { Accept: 'application/json' } });
                    if (!response.ok || !response.headers.get('content-type')?.includes('application/json')) throw new Error('Search unavailable');
                    const contacts = await response.json();
                    if (id !== requestId) return;
                    if (!contacts.length) { message('No matching contacts.'); return; }
                    results.replaceChildren();
                    contacts.forEach(contact => {
                        const link = document.createElement('a');
                        link.href = '/user/' + encodeURIComponent(contact.cId) + '/contact';
                        link.textContent = contact.cName;
                        results.append(link);
                    });
                    results.style.display = 'block';
                    input.setAttribute('aria-expanded', 'true');
                } catch (error) {
                    if (error.name !== 'AbortError' && id === requestId) message('Search is unavailable. Please try again or sign in again.');
                }
            }, 220);
        });
        input.addEventListener('keydown', event => {
            if (event.key === 'Escape') { ++requestId; controller?.abort(); clearTimeout(timer); close(); }
            if (event.key === 'ArrowDown') { event.preventDefault(); results.querySelector('a')?.focus(); }
        });
        document.addEventListener('click', event => {
            if (!input.parentElement.contains(event.target)) { ++requestId; clearTimeout(timer); controller?.abort(); close(); }
        });
    });
    document.addEventListener('keydown', event => {
        if (event.key === 'Escape' && document.body.classList.contains('sidebar-open')) {
            toggleSidebar();
            document.querySelector('.sidebar-toggle')?.focus();
        }
    });
});
