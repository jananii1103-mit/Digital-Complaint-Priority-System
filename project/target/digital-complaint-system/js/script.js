
// Client-side interactions and filtering for admin table & validation
document.addEventListener('DOMContentLoaded', function() {
    const searchInput = document.getElementById('searchComplaintId');
    const categoryFilter = document.getElementById('filterCategory');
    const priorityFilter = document.getElementById('filterPriority');
    const statusFilter = document.getElementById('filterStatus');

    if (searchInput || categoryFilter || priorityFilter || statusFilter) {
        const tableRows = document.querySelectorAll('.complaint-row');

        function filterTable() {
            const searchTerm = searchInput ? searchInput.value.toLowerCase().trim() : '';
            const catVal = categoryFilter ? categoryFilter.value : '';
            const priVal = priorityFilter ? priorityFilter.value : '';
            const staVal = statusFilter ? statusFilter.value : '';

            tableRows.forEach(row => {
                const id = row.getAttribute('data-id').toLowerCase();
                const title = row.getAttribute('data-title').toLowerCase();
                const cat = row.getAttribute('data-category');
                const pri = row.getAttribute('data-priority');
                const sta = row.getAttribute('data-status');

                const matchesSearch = id.includes(searchTerm) || title.includes(searchTerm);
                const matchesCat = !catVal || cat === catVal;
                const matchesPri = !priVal || pri === priVal;
                const matchesSta = !staVal || sta === staVal;

                if (matchesSearch && matchesCat && matchesPri && matchesSta) {
                    row.style.display = '';
                } else {
                    row.style.display = 'none';
                }
            });
        }

        if (searchInput) searchInput.addEventListener('input', filterTable);
        if (categoryFilter) categoryFilter.addEventListener('change', filterTable);
        if (priorityFilter) priorityFilter.addEventListener('change', filterTable);
        if (statusFilter) statusFilter.addEventListener('change', filterTable);
    }
});
