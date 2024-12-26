let page = 0;
const size = 10;
const loadMoreReviewsBtn = document.createElement('button');

async function getNewReviews() {
  let status;
  return await fetch(`/api/public/get-reviews?page=${page}&size=${size}`, {
    headers: {'Content-Type': 'application/json'},
    method: 'get'
  }).then((response) => {
    console.log(response);
    status = response.status;
    if (status !== 204)
      return response.json();
    else
      return {ok: true, status: status};
  }).then((data) => {
    if (status === 200) {
      page = Math.min(page + 1, data['totalPages']);
      return {
        response: data['reviews'],
        status: status,
        ok: true,
      };
    }
    data.ok = false;
    data.status = status;
    console.log(data);
    return data;
  });
}

async function addNewReviews() {
  let reviews = await getNewReviews();
  console.log(reviews);
  if (reviews.status === 200) {
    reviews.response.forEach((r) => {
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
        images += `<img class="border-1 rounded-1 p-0 overflow-hidden ms-1 gallery-item" src="images\\${src}">`);
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
      reviewsSection.removeChild(loadMoreReviewsBtn);
      reviewsSection.appendChild(loadMoreReviewsBtn);
    });
  } else if (reviews.status === 204) {
    createPopup("Nothing more to load :(", messageLevel.INFO);
  } else {
    createPopup(reviews.response, messageLevel.ERROR);
  }

}

document.addEventListener('utilsInitiated', () => {
  loadMoreReviewsBtn.className = 'btn btn-primary d-block mb-5 me-auto ms-auto mt-3';
  loadMoreReviewsBtn.innerHTML = 'Load more reviews';
  reviewsSection = document.getElementById('reviews');
  reviewsSection.appendChild(loadMoreReviewsBtn);
  loadMoreReviewsBtn.addEventListener('click', addNewReviews);
  addNewReviews().then(_ => {
  });
});