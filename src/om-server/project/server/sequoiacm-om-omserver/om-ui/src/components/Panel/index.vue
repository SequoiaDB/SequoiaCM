<template>
  <div class="card panel">
    <header
      v-if="title"
      class="card-header"
    >
      <p class="card-header-title">
        <span v-text="title" />
      </p>
      <div class="card-header-icon" v-if="$slots['actions']">
        <slot name="actions" />
      </div>
    </header>
    <div v-if="$slots['default']" class="card-content">
      <slot />
    </div>
  </div>
</template>

<script>
  export default {
    props: {
      title: {
        type: String,
        required: false
      }
    }
  }
</script>

<style scoped>
.panel:not(:last-child) {
    margin-bottom: 1.5rem;
}
.panel {
    margin-bottom: 1.5rem;
}

.panel {
    font-size: 1rem;
}
.card {
    background-color: #fff;
    box-shadow: 0 2px 3px rgb(10 10 10 / 10%), 0 0 0 1px rgb(10 10 10 / 10%);
    color: #4a4a4a;
    max-width: 100%;
    position: relative;
}

.card-header, .card-header-title {
    display: flex;
}
.card-header {
    background-color: transparent;
    align-items: stretch;
    box-shadow: 0 1px 2px rgb(10 10 10 / 10%);
}
.card-header-title {
    -webkit-box-align: center;
    -ms-flex-align: center;
    align-items: center;
    color: #363636;
    flex-grow: 1;
    font-weight: 700;
    padding: .75rem;
}
.card-header, .card-header-title {
    display: flex;
}
.panel__close {
    margin-right: .75em;
    color: #b5b5b5;
    display: flex;
    align-items: center;
    justify-self: flex-end;
}
.card-content {
    background-color: transparent;
    padding: 1.5rem;
}
</style>
