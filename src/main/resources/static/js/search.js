function createPagination(currentPage, maxPage, handler) {
  const paginationContainer = document.getElementById('pagination');
  paginationContainer.innerHTML = ''; // Clear existing pagination
  if (maxPage === 0) return;
  const createButton = (text, buttonPage, disabled = false) => {
    const button = document.createElement('button');
    button.className = 'btn ' + (buttonPage === page ? 'btn-secondary' : 'btn-outline-secondary');
    button.textContent = text;
    button.disabled = disabled;
    button.onclick = () => handler(buttonPage);
    return button;
  };


  if (currentPage > 0) {
    paginationContainer.appendChild(createButton('First', 0));
    paginationContainer.appendChild(createButton('Prev', currentPage - 1));
  }

  if (currentPage <= 2) {
    for (let i = 0; i <= Math.min(2, maxPage); i++) {
      paginationContainer.appendChild(createButton(i + 1, i, i === currentPage));
    }
    if (maxPage > 2) {
      paginationContainer.appendChild(document.createTextNode('...'));
      paginationContainer.appendChild(createButton(maxPage + 1, maxPage));
    }
  } else if (currentPage >= maxPage - 2) {
    paginationContainer.appendChild(createButton(1, 0));
    if (currentPage > 3) {
      paginationContainer.appendChild(document.createTextNode('...'));
    }
    for (let i = Math.max(maxPage - 2, 0); i <= maxPage; i++) {
      paginationContainer.appendChild(createButton(i + 1, i, i === currentPage));
    }
  } else {
    paginationContainer.appendChild(createButton(1, 0));
    paginationContainer.appendChild(document.createTextNode('...'));
    for (let i = currentPage - 1; i <= currentPage + 1; i++) {
      paginationContainer.appendChild(createButton(i + 1, i, i === currentPage));
    }
    paginationContainer.appendChild(document.createTextNode('...'));
    paginationContainer.appendChild(createButton(maxPage + 1, maxPage));
  }

  if (currentPage < maxPage) {
    paginationContainer.appendChild(createButton('Next', currentPage + 1));
    paginationContainer.appendChild(createButton('Last', maxPage));
  }
}

const page = parseInt(get('page') || '0', 10);
const id = parseInt(location.pathname.slice(1).split('/')[1])
const type = location.pathname.slice(1).split('/')[0]
let subcategoriesList;
let totalPages = parseInt(document.querySelector('meta[name="_total_pages"]')?.getAttribute("value"), 10) - 1;
document.addEventListener('utilsInitiated', () => {
  subcategoriesList = document.getElementById('subcategories');
  const username = document.getElementById('visibleName');
  if (username) {
    const profileAvatar = document.getElementById('profileAvatar');
    if (profileAvatar.nodeName.toLowerCase() === 'div')
      profileAvatar.replaceWith(
        generateAvatar(username.textContent.trim() || "User", 'avatar-lg', false));
  }

  if (type === 'category') {
    getNewReviews(page).then();
    fillCategoryPath(id);
    fillSubcategories(id);
  }
  else createPagination(page, totalPages, goToPage);
});
const goToPage = (page) => {
  console.log(`Navigating to page ${page}`);
  location.search = "?page=" + page;
};

function fillSubcategories(categoryId) {
  if (!subcategoriesList) return;
  const children = getAllDescendants(categoryId, 1);
  children.delete(categoryId);
  subcategoriesList.innerHTML = Array.from(children.keys()).map((id) => {
    const cat = categories.get(id);
    return `<a class="rounded-5 pb-1 pt-1 ps-3 pe-3 alert text-decoration-none text-dark"
         href="/category/${cat.id}">${cat.name}</a>`
  }).join('');
}

async function getNewReviews(page = 0, size = 10) {
  try {
    const response = await sendFetch({page: page, size: size, categoryIds: Array.from(getAllDescendants(id).keys())},
      '/api/public/get-reviews-by-categories/', 'post');
    if (response.ok && !response.redirected) {
      totalPages = response.totalPages - 1;
      createPagination(response.page, totalPages, getNewReviews);
      reviewsSection.innerHTML = '';
      addNewReviews(response.reviews);
      if (!response.hasNext)
        createPopup("Nothing more to load :(", messageLevel.INFO);
      if (response.reviews.length === 0) {
        reviewsSection.innerHTML = `<h4>No reviews found...</h4>`;
      }
    }

  } catch (e) {
    createPopup("Error loading reviews: " + e, messageLevel.ERROR);
  }

}

function addNewReviews(reviews) {
  console.log(reviews);
  reviews.forEach((r) => {
    let div = document.createElement('div');
    let rating = Number(r['rating']);
    let imagesLeft = Number(r['totalImages']) - 3;
    let imagesLeftCloud = imagesLeft > 0 ?
      `<p class="alert bg-white end-0 m-0 p-2 position-absolute rounded-5 
text-center text-dark top-50 translate-middle">+${formatLikes(imagesLeft)} Photo${imagesLeft > 1 ? 's' : ''}</p>` : '';
    let imagePaths = r['imagePaths'];
    let avatar = r['authorAvatarPath'] ?
      `<img src="/images/${r['authorAvatarPath']}" class="avatar-xs me-1 rounded-circle">` :
      generateAvatar(r['authorName'], 'avatar-xs me-1').outerHTML;
    let userHref = r['authorId'] !== -1 ? `href="/user/${r['authorId']}"` : '';
    let images = '';
    for (let i = imagePaths.length; i < 3; ++i) {
      images += `<div class="border-1 rounded-1 p-0 overflow-hidden ms-1 gallery-item"></div>`
    }
    imagePaths.forEach(src =>
      images += `<img class="border-1 rounded-1 p-0 overflow-hidden ms-1 gallery-item" src="/images/${src}">`);
    div.className = 'card mb-4';
    div.innerHTML = `
<div class="card-header flex-vcenter justify-content-between">
  <h4 class="m-0"><a href="/review/${r['id']}" class="text-decoration-none text-black">${r['title']}</a></h4>
  <span class="fw-bolder m-0"><span class="align-text-top h5 lh-1 mb-0 me-1">${r['likes']}</span><i class="bi bi-heart-fill align-baseline text-danger"></i></span>
</div>
<div class="card-body row">
  <div class="col-12 col-md-6 float-start"><h5>${r['shortReview']}</h5><p>${r['reviewTeaser']}</p></div>
  <div class="col-12 col-md-6 d-flex d-inline-flex justify-content-end position-relative">${imagesLeftCloud}${images}</div>
</div>
<div class="card-footer d-flex justify-content-between align-items-center">
  <span class="star-rating">${'<i class="bi bi-star-fill star-off"></i>'.repeat(5 - rating)}${'<i class="bi bi-star-fill star-on"></i>'.repeat(rating)}
  </span>
    <a ${userHref} class="d-flex text-decoration-none text-black">${avatar}${r['authorName']}</a>
  <span class="date">${formatDateTime(r['createdAt'])}</span>
</div>
    `;
    reviewsSection.appendChild(div);
  });
}